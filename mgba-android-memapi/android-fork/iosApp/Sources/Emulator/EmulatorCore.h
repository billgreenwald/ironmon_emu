#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// GBA key bitmask — bit position = mGBA GBAKey enum value (see mgba/internal/gba/input.h).
typedef NS_OPTIONS(uint32_t, GBAKeyMask) {
    GBAKeyA      = 1u << 0,
    GBAKeyB      = 1u << 1,
    GBAKeySelect = 1u << 2,
    GBAKeyStart  = 1u << 3,
    GBAKeyRight  = 1u << 4,
    GBAKeyLeft   = 1u << 5,
    GBAKeyUp     = 1u << 6,
    GBAKeyDown   = 1u << 7,
    GBAKeyR      = 1u << 8,
    GBAKeyL      = 1u << 9,
};

/// Owns a live mGBA `mCore` and drives it single-threaded via `runFrame` on a dedicated thread.
/// The iOS analogue of the Android `runGame.cpp` layer. Video/audio/input/memory are exposed to
/// Swift; the tracker's memory reads go through `readMemoryAtAddress:length:` (lock-free, like
/// Android's `getMemoryRange`).
@interface EmulatorCore : NSObject

@property (nonatomic, readonly) NSInteger videoWidth;   // 240 for GBA
@property (nonatomic, readonly) NSInteger videoHeight;  // 160 for GBA
@property (nonatomic, readonly) BOOL isRunning;

/// Load a ROM (and attach its `.sav` next to it) and reset the core. Returns NO on failure.
- (BOOL)loadROMAtPath:(NSString *)romPath;

/// Start / stop the emulation thread.
- (void)start;
- (void)stop;

/// Current pressed-key bitmask (GBAKeyMask), applied each frame.
- (void)setKeys:(uint32_t)mask;

/// Copy the latest complete frame (RGBA8, videoWidth*videoHeight*4 bytes) into `dst`.
/// Returns NO if no frame is available or capacity is too small.
- (BOOL)copyFrameInto:(void *)dst capacity:(NSInteger)capacityBytes;

/// Pull up to `frames` interleaved-stereo Int16 sample-frames into `dst` (dst holds frames*2
/// int16). Returns the number of stereo frames actually written (caller zero-fills the rest).
- (NSInteger)readAudioInto:(int16_t *)dst frames:(NSInteger)frames;

/// Lock-free guest-memory read for the tracker: rawRead8 for ROM (0x08000000..<0x0E000000),
/// busRead8 otherwise. Returns nil if the core isn't running.
- (nullable NSData *)readMemoryAtAddress:(uint32_t)address length:(uint32_t)length;

@end

NS_ASSUME_NONNULL_END
