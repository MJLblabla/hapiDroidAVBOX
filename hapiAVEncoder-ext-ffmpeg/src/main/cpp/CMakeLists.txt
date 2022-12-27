cmake_minimum_required(VERSION 3.18.1)
# Declares and names the project.

project("avencoderffmpeg")

set(jnilibs ${CMAKE_SOURCE_DIR}/../jniLibs)
set(libname avencoderffmpeg)

include_directories(
        include
        common
        util
        encoder
        jni
)

link_directories(
        ${jnilibs}/${ANDROID_ABI}
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.


file(GLOB src-files
        ${CMAKE_SOURCE_DIR}/*.cpp
        ${CMAKE_SOURCE_DIR}/common/*.cpp
        ${CMAKE_SOURCE_DIR}/encoder/*.cpp
        ${CMAKE_SOURCE_DIR}/jni/*.cpp
        )

add_library( # Sets the name of the library.
        ${libname}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${src-files}
       )

set(third-party-libs_ffmpeg
        avutil
        avcodec
        x264
        )

set(native-libs
        log
        )
target_link_libraries( # Specifies the target library.
        ${libname}
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        ${third-party-libs_ffmpeg}
        ${native-libs}
        )
