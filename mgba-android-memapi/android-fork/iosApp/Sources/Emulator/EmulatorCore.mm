#import "EmulatorCore.h"

#include <mgba/core/core.h>
#include <mgba/core/config.h>
#include <mgba/core/interface.h>   // color_t
#include <mgba/core/blip_buf.h>

#include <atomic>
#include <cstdlib>
#include <cstring>
#include <pthread.h>
#include <os/lock.h>
#include <mach/mach_time.h>

static const uint32_t kRomStart = 0x08000000;
static const uint32_t kRomEnd   = 0x0E000000;
static const double   kFrameHz  = 59.7275;          // GBA refresh
static const int      kOutSampleRate = 48000;
static const int      kAudioRingFrames = 48000;     // ~1s stereo ring

// Simple single-producer / single-consumer int16 ring (producer = emu thread, consumer = audio).
namespace {
struct AudioRing {
    int16_t buf[kAudioRingFrames * 2];
    std::atomic<uint32_t> head{0};   // write index (samples, not frames)
    std::atomic<uint32_t> tail{0};   // read index
    static const uint32_t cap = kAudioRingFrames * 2;

    void push(const int16_t* src, uint32_t n) {
        uint32_t h = head.load(std::memory_order_relaxed);
        uint32_t t = tail.load(std::memory_order_acquire);
        uint32_t free = cap - (h - t);
        if (n > free) n = free;                 // drop on overflow
        for (uint32_t i = 0; i < n; ++i) buf[(h + i) % cap] = src[i];
        head.store(h + n, std::memory_order_release);
    }
    // pops up to n samples, returns count popped
    uint32_t pop(int16_t* dst, uint32_t n) {
        uint32_t t = tail.load(std::memory_order_relaxed);
        uint32_t h = head.load(std::memory_order_acquire);
        uint32_t avail = h - t;
        if (n > avail) n = avail;
        for (uint32_t i = 0; i < n; ++i) dst[i] = buf[(t + i) % cap];
        tail.store(t + n, std::memory_order_release);
        return n;
    }
};
}

@implementation EmulatorCore {
    struct mCore* _core;

    // video double-buffer
    color_t* _buffers[2];
    int _backIndex;                 // buffer the core renders into
    int _frontIndex;               // last completed buffer
    BOOL _hasFrame;
    os_unfair_lock _videoLock;
    size_t _frameBytes;

    // audio
    AudioRing _audio;

    // input
    std::atomic<uint32_t> _keys;
    std::atomic<float> _speed;      // wall-clock pacing multiplier (1.0 = normal)

    // thread
    std::atomic<bool> _running;
    pthread_t _thread;
}

- (instancetype)init {
    if ((self = [super init])) {
        _videoLock = OS_UNFAIR_LOCK_INIT;
        _keys.store(0);
        _speed.store(1.0f);
        _running.store(false);
        _backIndex = 0;
        _frontIndex = 0;
        _hasFrame = NO;
    }
    return self;
}

- (void)dealloc {
    [self stop];
    [self teardownCore];
}

- (void)teardownCore {
    if (_core) {
        _core->unloadROM(_core);
        mCoreConfigDeinit(&_core->config);
        _core->deinit(_core);
        _core = nullptr;
    }
    for (int i = 0; i < 2; ++i) {
        if (_buffers[i]) { free(_buffers[i]); _buffers[i] = nullptr; }
    }
}

- (BOOL)loadROMAtPath:(NSString *)romPath {
    [self stop];
    [self teardownCore];

    _core = mCoreFind(romPath.UTF8String);
    if (!_core) return NO;
    if (!_core->init(_core)) { _core = nullptr; return NO; }
    mCoreInitConfig(_core, "ios");

    unsigned w = 0, h = 0;
    _core->desiredVideoDimensions(_core, &w, &h);
    _videoWidth = (NSInteger)w;
    _videoHeight = (NSInteger)h;
    _frameBytes = (size_t)w * h * sizeof(color_t);

    for (int i = 0; i < 2; ++i) {
        _buffers[i] = (color_t*)calloc((size_t)w * h, sizeof(color_t));
    }
    _backIndex = 0;
    _frontIndex = 1;
    _hasFrame = NO;
    _core->setVideoBuffer(_core, _buffers[_backIndex], w);
    _core->setAudioBufferSize(_core, 2048);

    if (!mCoreLoadFile(_core, romPath.UTF8String)) {
        [self teardownCore];
        return NO;
    }
    // Attach SRAM save next to the ROM (already inside the app sandbox).
    NSString* save = [[romPath stringByDeletingPathExtension] stringByAppendingPathExtension:@"sav"];
    mCoreLoadSaveFile(_core, save.UTF8String, false);

    _core->reset(_core);
    return YES;
}

- (void)setKeys:(uint32_t)mask { _keys.store(mask, std::memory_order_relaxed); }

- (void)setSpeedMultiplier:(float)mult {
    if (mult < 0.25f) mult = 0.25f;
    _speed.store(mult, std::memory_order_relaxed);
}

- (BOOL)isRunning { return _running.load(); }

- (void)start {
    if (!_core || _running.load()) return;
    _running.store(true);
    pthread_create(&_thread, nullptr, &EmulatorCore_run, (__bridge void*)self);
}

- (void)stop {
    if (!_running.load()) return;
    _running.store(false);
    pthread_join(_thread, nullptr);
}

static void* EmulatorCore_run(void* ctx) {
    EmulatorCore* self = (__bridge EmulatorCore*)ctx;
    [self emulationLoop];
    return nullptr;
}

- (void)emulationLoop {
    pthread_setname_np("mgba-emulation");
    mach_timebase_info_data_t tb; mach_timebase_info(&tb);
    uint64_t nextDeadline = mach_absolute_time();
    int16_t temp[4096];

    while (_running.load()) {
        float speed = _speed.load(std::memory_order_relaxed);
        const double nanosPerFrame = 1e9 / (kFrameHz * speed);
        _core->setKeys(_core, _keys.load(std::memory_order_relaxed));
        _core->runFrame(_core);

        // publish completed frame, flip buffers
        os_unfair_lock_lock(&_videoLock);
        int done = _backIndex;
        _frontIndex = done;
        _backIndex ^= 1;
        _hasFrame = YES;
        _core->setVideoBuffer(_core, _buffers[_backIndex], (size_t)_videoWidth);
        os_unfair_lock_unlock(&_videoLock);

        // drain audio: resample core-clock -> 48k, interleave L/R, push to ring
        blip_t* left  = _core->getAudioChannel(_core, 0);
        blip_t* right = _core->getAudioChannel(_core, 1);
        double clock = (double)_core->frequency(_core);
        blip_set_rates(left,  clock, kOutSampleRate);
        blip_set_rates(right, clock, kOutSampleRate);
        // Always drain the blip buffers so they don't back up, but only forward samples to the
        // output ring near normal speed — fast-forward would otherwise overflow it into garble.
        BOOL audible = (speed <= 1.5f);
        int avail = blip_samples_avail(left);
        while (avail > 0) {
            int n = avail < 2048 ? avail : 2048;   // n stereo frames
            blip_read_samples(left,  temp,     n, 1);
            blip_read_samples(right, temp + 1, n, 1);
            if (audible) _audio.push(temp, (uint32_t)(n * 2));
            avail = blip_samples_avail(left);
        }

        // pace to ~59.7275 Hz (wall clock)
        nextDeadline += (uint64_t)(nanosPerFrame * tb.denom / tb.numer);
        uint64_t now = mach_absolute_time();
        if (nextDeadline > now) {
            uint64_t waitNanos = (nextDeadline - now) * tb.numer / tb.denom;
            struct timespec ts { (time_t)(waitNanos / 1000000000ull),
                                 (long)(waitNanos % 1000000000ull) };
            nanosleep(&ts, nullptr);
        } else {
            nextDeadline = now;   // fell behind; don't accumulate debt
        }
    }
}

- (BOOL)copyFrameInto:(void *)dst capacity:(NSInteger)capacityBytes {
    if ((size_t)capacityBytes < _frameBytes) return NO;
    os_unfair_lock_lock(&_videoLock);
    BOOL ok = _hasFrame;
    if (ok) memcpy(dst, _buffers[_frontIndex], _frameBytes);
    os_unfair_lock_unlock(&_videoLock);
    return ok;
}

- (NSInteger)readAudioInto:(int16_t *)dst frames:(NSInteger)frames {
    uint32_t popped = _audio.pop(dst, (uint32_t)(frames * 2));  // samples
    return (NSInteger)(popped / 2);                             // stereo frames
}

- (NSData *)readMemoryAtAddress:(uint32_t)address length:(uint32_t)length {
    if (!_core || !_running.load()) return nil;
    NSMutableData* data = [NSMutableData dataWithLength:length];
    uint8_t* out = (uint8_t*)data.mutableBytes;
    for (uint32_t i = 0; i < length; ++i) {
        uint32_t a = address + i;
        uint32_t b = (a >= kRomStart && a < kRomEnd)
            ? _core->rawRead8(_core, a, -1)
            : _core->busRead8(_core, a);
        out[i] = (uint8_t)(b & 0xFF);
    }
    return data;
}

@end
