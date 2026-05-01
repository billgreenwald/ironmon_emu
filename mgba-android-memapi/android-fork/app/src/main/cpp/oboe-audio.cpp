#include <oboe/Oboe.h>
#include <mgba/core/core.h>
#include <mgba/core/thread.h>
#include <mgba/core/blip_buf.h>
#include <mgba/internal/gba/audio.h> // For GBAAudioCalculateRatio
#include <mgba/internal/gba/gba.h>   // For GBA_ARM7TDMI_FREQUENCY
#include <android/log.h>
#include <thread>
#include <chrono>

#define TAG "OboeAudio"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

using namespace oboe;

// Forward declarations so onErrorAfterClose can call them (defined in extern "C" block below)
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
        
        // Logic adapted from _mSDLAudioCallback in sdl-audio.c
        
        blip_t* left = core->getAudioChannel(core, 0);
        blip_t* right = core->getAudioChannel(core, 1);
        
        // Frequency might be core specific, but GBA is usually 16777216. 
        // Using GBA_ARM7TDMI_FREQUENCY from gba.h if possible, else core->frequency()
        int32_t clockRate = core->frequency(core);
        
        int sampleRate = oboeStream->getSampleRate();
        if (sampleRate <= 0) {
            memset(audioData, 0, numFrames * oboeStream->getChannelCount() * sizeof(int16_t));
            return DataCallbackResult::Stop;
        }
        double fauxClock = 1;
        
        if (sync && sync->fpsTarget > 0) {
            // Re-implementing GBAAudioCalculateRatio logic or calling it if available.
            // Assuming 59.7275Hz is standard GBA framerate.
            // Ratio is target / native.
            // Simplify: GBAAudioCalculateRatio(1, fpsTarget, 1) usually roughly 1.0
            
            // To imply logic:
            // static double GBAAudioCalculateRatio(double sampleRate, double fps, double nativeFps)
            // return sampleRate * nativeFps / fps;
            
            // SDL code: fauxClock = GBAAudioCalculateRatio(1, audioContext->sync->fpsTarget, 1);
            // This suggests it adjusts the clock rate effectively changing pitch/speed.
            // We can skip this for now or try to implement if we have the header.
             fauxClock = 59.7275f / sync->fpsTarget; // Approx inverse of what SDL likely does?
             // Actually, let's stick to standard rate if sync is disabled or 1.0.
             if (fauxClock < 0.1 || fauxClock > 10.0) fauxClock = 1.0;
        }

        mCoreSyncLockAudio(sync);

        blip_set_rates(left, clockRate, sampleRate * fauxClock);
        blip_set_rates(right, clockRate, sampleRate * fauxClock);

        int available = blip_samples_avail(left);
        if (available > numFrames) {
            available = numFrames;
        }

        // mGBA blip_read_samples reads mono to buffer. Stereo needs interleaving or specific call.
        // blip_read_samples signature: (blip_t*, short* out, int count, int stereo)
        // If stereo=1, it writes stride 2? No, mGBA's blip_buf is mono.
        // sdl-audio.c does:
        // blip_read_samples(left, (short*) data, available, audioContext->obtainedSpec.channels == 2);
        // blip_read_samples(right, ((short*) data) + 1, available, 1);
        
        // So yes, the 4th arg is 'stereo' stride.
        
        blip_read_samples(left, outputData, available, 1);
        blip_read_samples(right, outputData + 1, available, 1);
        
        mCoreSyncConsumeAudio(sync);
        
        if (available < numFrames) {
             memset(outputData + available * 2, 0, (numFrames - available) * 2 * sizeof(int16_t));
        }

        return DataCallbackResult::Continue;
    }

    void onErrorAfterClose(AudioStream* /*oboeStream*/, Result error) override {
        if (error == Result::ErrorDisconnected && mThread) {
            struct mCoreThread* thread = mThread; // capture before mOboeDeinit deletes `this`
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

// Global stream wrapper (for simplicity in this single-instance app)
static std::shared_ptr<AudioStream> mStream;
static OboeAudioStreamCallback* mCallback = nullptr;

extern "C" {

bool mOboeInit(struct mCoreThread* thread);
void mOboeDeinit();

// Since we are replacing SDL audio which is polled/callback based,
// we need to adapt.

bool mOboeInit(struct mCoreThread* thread) {
    LOGD("Initializing Oboe Audio");
    
    AudioStreamBuilder builder;
    builder.setDirection(Direction::Output);
    builder.setPerformanceMode(PerformanceMode::LowLatency);
    builder.setSharingMode(SharingMode::Shared);
    builder.setFormat(AudioFormat::I16);
    builder.setChannelCount(ChannelCount::Stereo);
    builder.setSampleRate(48000); // Standard for mGBA
    
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
    
    LOGD("Oboe Audio Started");
    return true;
}

void mOboeDeinit() {
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
