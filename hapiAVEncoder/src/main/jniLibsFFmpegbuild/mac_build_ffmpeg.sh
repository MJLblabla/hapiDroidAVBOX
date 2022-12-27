#!/bin/bash

API=21
NDK=/Users/manjiale/Library/Android/sdk/ndk/21.4.7075529
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/darwin-x86_64

cd ffmpeg
X264_PREFIX="$(dirname "$PWD")/build/x264"
ANDROID_LIB_PATH="$(dirname "$PWD")/build/ffmpeg"

echo  "${X264_PREFIX}"
echo "$(dirname "$PWD")"
echo "$("$PWD")"

function build_android() {
  ./configure \
    --prefix=$PREFIX \
    --disable-opencl \
    --disable-doc \
    --disable-everything \
    --disable-htmlpages \
    --disable-podpages \
    --disable-debug \
    --disable-programs \
    --disable-demuxers \
    --disable-muxers \
    --disable-decoders \
    --disable-asm \
    --disable-shared \
    --pkg-config=pkg-config \
    --disable-avdevice \
    --disable-postproc \
    --disable-avfilter \
    --disable-symver \
    --disable-swscale \
    --enable-jni \
    --enable-static \
    \
    --enable-avformat \
    \
    --disable-encoders \
    --enable-gpl \
    --disable-nonfree \
    --enable-libx264 \
    --enable-encoder=libx264 \
    --enable-encoder=aac \
    --disable-encoder=opus \
     \
    --enable-swresample  \
     \
    --enable-small \
    --cross-prefix=$CROSS_PREFIX \
    --target-os=android \
    --arch=$ARCH \
    --cpu=$CPU \
    --cc=$CC \
    --cxx=$CXX \
    --enable-cross-compile \
    --sysroot=$SYSROOT \
    --extra-cflags="-I${X264_INCLUDE}  -Os -fpic $OPTIMIZE_CFLAGS" \
    --extra-ldflags="-L${X264_LIB}  $ADDI_LDFLAGS" || exit 1
  make clean
  make -j8
  make install
}

#armv8-a
ARCH=arm64
CPU=armv8-a

X264_INCLUDE=${X264_PREFIX}/$CPU/include
X264_LIB=${X264_PREFIX}/$CPU/lib

CC=$TOOLCHAIN/bin/aarch64-linux-android$API-clang
CXX=$TOOLCHAIN/bin/aarch64-linux-android$API-clang++
SYSROOT=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/aarch64-linux-android-
PREFIX=${ANDROID_LIB_PATH}/$CPU
OPTIMIZE_CFLAGS="-march=$CPU"
build_android


#armv7-a
ARCH=arm
CPU=armv7-a

X264_INCLUDE=${X264_PREFIX}/$CPU/include
X264_LIB=${X264_PREFIX}/$CPU/lib

CC=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang
CXX=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
SYSROOT=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/arm-linux-androideabi-
PREFIX=${ANDROID_LIB_PATH}/$CPU
OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=vfp -marm -march=$CPU "
build_android

#x86
ARCH=x86
CPU=x86


X264_INCLUDE=${X264_PREFIX}/$CPU/include
X264_LIB=${X264_PREFIX}/$CPU/lib

CC=$TOOLCHAIN/bin/i686-linux-android$API-clang
CXX=$TOOLCHAIN/bin/i686-linux-android$API-clang++
SYSROOT=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/i686-linux-android-
PREFIX=${ANDROID_LIB_PATH}/$CPU
OPTIMIZE_CFLAGS="-march=i686 -mtune=intel -mssse3 -mfpmath=sse -m32"
build_android

#x86_64
ARCH=x86_64
CPU=x86-64

X264_INCLUDE=${X264_PREFIX}/$CPU/include
X264_LIB=${X264_PREFIX}/$CPU/lib

CC=$TOOLCHAIN/bin/x86_64-linux-android$API-clang
CXX=$TOOLCHAIN/bin/x86_64-linux-android$API-clang++
SYSROOT=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot
CROSS_PREFIX=$TOOLCHAIN/bin/x86_64-linux-android-
PREFIX=${ANDROID_LIB_PATH}/$CPU
OPTIMIZE_CFLAGS="-march=$CPU -msse4.2 -mpopcnt -m64 -mtune=intel"
build_android