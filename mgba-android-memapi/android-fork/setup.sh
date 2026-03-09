#!/usr/bin/env bash
# Clone external Android dependencies (not tracked in this repo).
# Run this once after cloning the repo before building the Android APK.

set -e
CPP=app/src/main/cpp

echo "Cloning huhao1987/mgba (android_port_0.10)..."
git clone --depth=1 --branch android_port_0.10 \
    https://github.com/huhao1987/mgba \
    "$CPP/mgba"

echo "Cloning huhao1987/SDL (SDL2_update_Android)..."
git clone --depth=1 --branch SDL2_update_Android \
    https://github.com/huhao1987/SDL \
    "$CPP/SDL2"

# Copy SDL Java sources into the app source tree
echo "Copying SDL Android Java sources..."
cp -r "$CPP/SDL2/android-project/app/src/main/java/org/libsdl/app/." \
    app/src/main/java/org/libsdl/app/

echo "Done. Now run: ANDROID_HOME=/path/to/sdk ./gradlew :app:assembleDebug"
