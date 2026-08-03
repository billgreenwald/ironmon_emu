#!/usr/bin/env bash
#
# Builds libmgba.xcframework (iOS device arm64 + simulator arm64) from the FORK's mGBA source
# (app/src/main/cpp/mgba — NOT upstream/, whose API differs). Runs on a macOS CI runner.
#
# The recipe is validated: on Linux, the same flags (LIBMGBA_ONLY + M_CORE_GBA, static) produce a
# working libmgba.a exporting mCoreFind / GBACoreCreate / the bus/raw read impls. Only the Apple
# sysroot/arch flags are added here.
#
# Usage:  ./build-libmgba.sh [output_dir]
#         default output_dir = iosApp/build/libmgba/libmgba.xcframework
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
MGBA="$(cd "$HERE/../../app/src/main/cpp/mgba" && pwd)"
OUT="${1:-$HERE/../build/libmgba}"
DEPLOY_TARGET="15.0"

jobs="$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)"

build_slice() {
  local sysroot="$1" arch="$2" tag="$3"
  local bdir="$OUT/build-$tag"
  echo ">>> Configuring libmgba for $tag ($sysroot / $arch)"
  cmake -S "$MGBA" -B "$bdir" \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT="$sysroot" \
    -DCMAKE_OSX_ARCHITECTURES="$arch" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOY_TARGET" \
    -DCMAKE_BUILD_TYPE=Release \
    -DLIBMGBA_ONLY=ON \
    -DBUILD_STATIC=ON \
    -DM_CORE_GBA=ON \
    -DM_CORE_GB=OFF
  echo ">>> Building libmgba for $tag"
  cmake --build "$bdir" --target mgba -j"$jobs"
}

build_slice iphoneos        arm64 device
build_slice iphonesimulator arm64 simulator

# Public headers = source include/ (mgba/ + mgba-util/) with the per-build generated flags.h overlaid.
HDR="$OUT/headers"
rm -rf "$HDR"; mkdir -p "$HDR"
cp -R "$MGBA/include/." "$HDR/"
cp "$OUT/build-device/include/mgba/flags.h" "$HDR/mgba/flags.h"

rm -rf "$OUT/libmgba.xcframework"
xcodebuild -create-xcframework \
  -library "$OUT/build-device/libmgba.a"    -headers "$HDR" \
  -library "$OUT/build-simulator/libmgba.a" -headers "$HDR" \
  -output  "$OUT/libmgba.xcframework"

echo ">>> Done: $OUT/libmgba.xcframework"
