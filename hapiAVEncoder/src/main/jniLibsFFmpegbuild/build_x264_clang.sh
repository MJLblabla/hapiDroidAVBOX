#!/bin/bash

#!/bin/bash
NDK_HOME=/Users/manjiale/Library/Android/sdk/ndk/21.4.7075529
HOST_TAG=darwin-x86_64
TOOLCHAIN=$NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG
SYSROOT=$TOOLCHAIN/sysroot

cd x264
ANDROID_LIB_PATH="$(dirname "$PWD")/build/x264"

API=21

function build_android_arm
{
echo "build for android $CPU"
./configure \
--prefix="$ANDROID_LIB_PATH/$CPU" \
--host=$HOST \
--disable-shared \
--enable-static \
--disable-cli \
--disable-asm \
--extra-cflags="$CFLAGS" \
--prefix="$ANDROID_LIB_PATH/$CPU" \
--sysroot="$SYSROOT" \
--cross-prefix="$CROSS_PREFIX"

make clean
make -j8
make install
echo "building for android $CPU completed"
}

# armv7-a
CPU=armv7-a
HOST=arm-linux
CROSS_PREFIX=$TOOLCHAIN/bin/arm-linux-androideabi-
CFLAGS="-march=armv7-a -O2 -mfloat-abi=softfp -mfpu=neon -fPIC"
export CC=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang
export CXX=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
build_android_arm


## armv8-a
CPU=armv8-a
HOST=arm-linux
CROSS_PREFIX=$TOOLCHAIN/bin/aarch64-linux-android-
CFLAGS="-O2 -mfloat-abi=softfp -mfpu=neon -fPIC"
export CC=$TOOLCHAIN/bin/aarch64-linux-android$API-clang
export CXX=$TOOLCHAIN/bin/aarch64-linux-android$API-clang++
build_android_arm

# armv8-a

CPU=x86
HOST=i686-linux
CROSS_PREFIX=$TOOLCHAIN/bin/i686-linux-android-
CFLAGS="-march=i686 -mtune=intel -mssse3 -mfpmath=sse -m32"
export CC=$TOOLCHAIN/bin/i686-linux-android$API-clang
export CXX=$TOOLCHAIN/bin/i686-linux-android$API-clang++

build_android_arm


CPU=x86-64
HOST=x86_64-linux
CROSS_PREFIX=$TOOLCHAIN/bin/x86_64-linux-android-
CFLAGS="-march=x86-64 -msse4.2 -mpopcnt -m64 -mtune=intel"
export CC=$TOOLCHAIN/bin/x86_64-linux-android$API-clang
export CXX=$TOOLCHAIN/bin/x86_64-linux-android$API-clang++

build_android_arm