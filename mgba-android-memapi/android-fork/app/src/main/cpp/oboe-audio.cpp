#include <oboe/Oboe.h>
#include <mgba/core/core.h>
#include <mgba/core/thread.h>
#include <mgba/core/blip_buf.h>
#include <mgba/internal/gba/audio.h>
#include <mgba/internal/gba/gba.h>
#include <android/log.h>
#include <thread>
#include <chrono>
#include <algorithm>

extern "C" {
#include "sonic.h"
}

#define TAG "OboeAudio"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

using namespace oboe;

static sonicStream gSonicStream = nullptr;
static bool gPrevUsedSonic = false;

// Temp buffers for interleaving blip mono channels before feeding Sonic
static int16_t gTempLeft[4096];
static int16_t gTempRight[4096];
static int16_t gTempInterleaved[4096 * 2];
static int16_t gDiscardBuf[4096];

// Forward declarations
extern "C" bool mOboeInit(struct mCoreThread* thread);
extern "C" void mOboeDeinit();

class OboeAudioStreamCallback : public AudioStreamCallback {
public:
    OboeAudioStreamCallback(struct mCoreThread* thread) : mThread(thread) {}

    DataCallbackResult onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        if (!mThread || !mThread->impl || !mThread->core) {
            memset(audioData, 0, numFrames * oboeStream->getChannelCount() * sizeof(int16_t));
            return DataCallbackResult::Continue;
        }

        int16_t *outputData = static_cast<int16_t *>(audioData);
        struct mCore* core = mThread->core;
        struct mCoreSync* sync = &mThread->impl->sync;

        blip_t* left  = core->getAudioChannel(core, 0);
        blip_t* right = core->getAudioChannel(core, 1);

        int32_t clockRate = core->frequency(core);
        int sampleRate = oboeStream->getSampleRate();
        if (sampleRate <= 0) {
            memset(audioData, 0, numFrames * oboeStream->getChannelCount() * sizeof(int16_t));
            return DataCallbackResult::Stop;
        }

        float speed = 1.0f;
        if (sync && sync->fpsTarget > 0) {
            speed = sync->fpsTarget / 59.7275f;
            if (speed < 0.1f || speed > 16.0f) speed = 1.0f;
        }

        // Only engage Sonic above 1x. At 1x: direct blip→output, zero latency.
        bool useSonic = (speed > 1.05f);

        // fauxClock scales blip's dest_rate so it produces exactly the right
        // number of samples per callback at any speed — this is what rate-limits
        // the emulator to the selected speed (2x ≠ 3x ≠ 4x).
        double fauxClock = useSonic ? (59.7275 / sync->fpsTarget) : 1.0;
        if (fauxClock < 0.1 || fauxClock > 10.0) fauxClock = 1.0;

        mCoreSyncLockAudio(sync);
        blip_set_rates(left,  clockRate, sampleRate * fauxClock);
        blip_set_rates(right, clockRate, sampleRate * fauxClock);

        int available = blip_samples_avail(left);
        if (available > numFrames) available = numFrames;

        if (useSonic && gSonicStream) {
            // Read blip samples (at Nx pitch due to fauxClock) into temp mono buffers.
            blip_read_samples(left,  gTempLeft,  available, 0);
            blip_read_samples(right, gTempRight, available, 0);
            mCoreSyncConsumeAudio(sync);

            // Pitch-only correction: shift down by 1/speed to undo the fauxClock
            // pitch shift. Speed and rate stay 1.0 so sample count is preserved.
            sonicSetSpeed(gSonicStream, 1.0f);
            sonicSetPitch(gSonicStream, 1.0f / speed);
            sonicSetRate(gSonicStream, 1.0f);

            for (int i = 0; i < available; i++) {
                gTempInterleaved[i * 2]     = gTempLeft[i];
                gTempInterleaved[i * 2 + 1] = gTempRight[i];
            }
            sonicWriteShortToStream(gSonicStream, gTempInterleaved, available);

            int got = sonicReadShortFromStream(gSonicStream, outputData, numFrames);
            if (got < numFrames) {
                memset(outputData + got * 2, 0, (numFrames - got) * 2 * sizeof(int16_t));
            }

        } else {
            // Direct path — original blip→output, no added latency.
            blip_read_samples(left,  outputData,     available, 1);
            blip_read_samples(right, outputData + 1, available, 1);
            mCoreSyncConsumeAudio(sync);

            if (available < numFrames) {
                memset(outputData + available * 2, 0,
                       (numFrames - available) * 2 * sizeof(int16_t));
            }

            // Flush Sonic's internal buffer on the first 1x callback after fast-forward
            // so its tail doesn't bleed through out of sync.
            if (gPrevUsedSonic && gSonicStream) {
                sonicFlushStream(gSonicStream);
                int avail;
                while ((avail = sonicSamplesAvailable(gSonicStream)) > 0) {
                    sonicReadShortFromStream(gSonicStream, gDiscardBuf,
                                            std::min(avail, 4096));
                }
            }
        }

        gPrevUsedSonic = useSonic;
        return DataCallbackResult::Continue;
    }

    void onErrorAfterClose(AudioStream* /*oboeStream*/, Result error) override {
        if (error == Result::ErrorDisconnected && mThread) {
            struct mCoreThread* thread = mThread;
            std::thread([thread]() {
                std::this_thread::sleep_for(std::chrono::milliseconds(200));
                mOboeDeinit();
                mOboeInit(thread);
            }).detach();
        }
    }

private:
    struct mCoreThread* mThread;
};

static std::shared_ptr<AudioStream> mStream;
static OboeAudioStreamCallback* mCallback = nullptr;

extern "C" {

bool mOboeInit(struct mCoreThread* thread) {
    LOGD("Initializing Oboe Audio");

    AudioStreamBuilder builder;
    builder.setDirection(Direction::Output);
    builder.setPerformanceMode(PerformanceMode::LowLatency);
    builder.setSharingMode(SharingMode::Shared);
    builder.setFormat(AudioFormat::I16);
    builder.setChannelCount(ChannelCount::Stereo);
    builder.setSampleRate(48000);

    mCallback = new OboeAudioStreamCallback(thread);
    builder.setCallback(mCallback);

    Result result = builder.openStream(mStream);
    if (result != Result::OK) {
        LOGE("Failed to open Oboe stream: %s", convertToText(result));
        return false;
    }

    result = mStream->requestStart();
    if (result != Result::OK) {
        LOGE("Failed to start Oboe stream: %s", convertToText(result));
        return false;
    }

    gSonicStream = sonicCreateStream(48000, 2);
    sonicSetSpeed(gSonicStream, 1.0f);
    sonicSetPitch(gSonicStream, 1.0f);
    sonicSetRate(gSonicStream, 1.0f);
    gPrevUsedSonic = false;

    LOGD("Oboe Audio Started");
    return true;
}

void mOboeDeinit() {
    if (gSonicStream) {
        sonicDestroyStream(gSonicStream);
        gSonicStream = nullptr;
    }
    if (mStream) {
        mStream->close();
        mStream.reset();
    }
    if (mCallback) {
        delete mCallback;
        mCallback = nullptr;
    }
}

}
