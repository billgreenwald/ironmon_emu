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

// Temp buffers for draining blip — sized for up to 8x speed (6400 frames)
static int16_t gTempLeft[6400];
static int16_t gTempRight[6400];
static int16_t gTempInterleaved[6400 * 2];

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

        // Speed multiplier: 1.0 = normal, 2.0 = 2x, etc.
        float speed = 1.0f;
        if (sync && sync->fpsTarget > 0) {
            speed = sync->fpsTarget / 59.7275f;
            if (speed < 0.1f || speed > 16.0f) speed = 1.0f;
        }

        mCoreSyncLockAudio(sync);

        // Always sample at native rate — Sonic handles tempo correction
        blip_set_rates(left,  clockRate, sampleRate);
        blip_set_rates(right, clockRate, sampleRate);

        // Drain ALL available samples to prevent blip overflow at high speeds,
        // which would otherwise block the emulator thread via mCoreSyncLockAudio.
        int available = blip_samples_avail(left);
        int toDrain = std::min(available, 6400);

        if (toDrain > 0) {
            blip_read_samples(left,  gTempLeft,  toDrain, 0);
            blip_read_samples(right, gTempRight, toDrain, 0);
            for (int i = 0; i < toDrain; i++) {
                gTempInterleaved[i * 2]     = gTempLeft[i];
                gTempInterleaved[i * 2 + 1] = gTempRight[i];
            }
            if (gSonicStream) {
                sonicSetSpeed(gSonicStream, speed);
                sonicWriteShortToStream(gSonicStream, gTempInterleaved, toDrain);
            }
        }

        mCoreSyncConsumeAudio(sync);

        int got = 0;
        if (gSonicStream) {
            got = sonicReadShortFromStream(gSonicStream, outputData, numFrames);
        }
        if (got < numFrames) {
            memset(outputData + got * 2, 0, (numFrames - got) * 2 * sizeof(int16_t));
        }

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
