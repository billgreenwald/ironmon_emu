# mGBA Android Memory API — Integration Notes

## What was changed vs upstream mGBA

### New files (entire feature lives here)
- `src/platform/android/src/main/jni/memapi_server.c` — TCP server implementation
- `src/platform/android/src/main/jni/memapi_server.h` — public interface

### Files requiring minimal modification after cloning mGBA

#### 1. Android JNI entry point
The exact filename depends on the mGBA version you clone. Common candidates:
- `src/platform/android/src/main/jni/GameActivity.cpp`
- `src/platform/android/src/main/jni/mGBA.cpp`

Run `grep -r "mCoreLoadFile\|mCoreRun\|mCoreCreate" src/platform/android/` to find it.

Add these two hooks (both wrapped in `#ifdef MEMAPI_ENABLE`):

```cpp
#include "memapi_server.h"

// After ROM is loaded and core is initialized:
#ifdef MEMAPI_ENABLE
    memapi_server_start(core);
#endif

// Before core teardown (e.g. in emulator stop / activity destroy):
#ifdef MEMAPI_ENABLE
    memapi_server_stop();
#endif
```

#### 2. `src/platform/android/CMakeLists.txt`
Apply the additions described in `CMakeLists_patch.cmake`.

#### 3. `app/build.gradle` (Android Gradle module)
```kotlin
android {
    defaultConfig {
        externalNativeBuild {
            cmake { arguments("-DCMAKE_BUILD_TYPE=Debug") }
        }
    }
    buildTypes {
        debug {
            externalNativeBuild {
                cmake { arguments("-DMEMAPI_ENABLE=ON") }
            }
        }
        release {
            // MEMAPI_ENABLE intentionally omitted — no server code in release APK
        }
    }
    externalNativeBuild {
        cmake {
            path = "src/platform/android/CMakeLists.txt"
        }
    }
}
```

## Testing the server without Android

Build mGBA for Linux desktop and link `memapi_server.c` against the desktop
target. Then from another terminal:

```bash
# Read 4 bytes from GBA ROM header (game code) at 0x080000AC
python3 - <<'EOF'
import socket, struct
addr = 0x080000AC
s = socket.socket()
s.connect(('127.0.0.1', 7777))
s.sendall(struct.pack('<IB', addr, 4) + b'\x00')
data = s.recv(4)
print("Game code:", data.decode('ascii', errors='replace'))
s.close()
EOF
```

Expected output for Fire Red: `BPRE`

## Port forwarding from Android device to laptop

```bash
adb forward tcp:7777 tcp:7777
# Now run the Python script above from your laptop
```

## Security notes
- Server only binds to `127.0.0.1` — not accessible from network
- No authentication needed (local only)
- Server thread is a pthread, not a Java thread — no JVM overhead
- `MEMAPI_ENABLE` guard ensures zero code in production builds
