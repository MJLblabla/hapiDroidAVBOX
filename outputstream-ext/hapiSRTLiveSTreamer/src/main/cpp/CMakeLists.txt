# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
# Declares and names the project.

project("srtmuxer")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(jnilibs ${CMAKE_SOURCE_DIR}/../jniLibs)
set(libname srtmuxer)

include_directories(
        jni
        include
        srt
        ../../../../iconnection
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
        ${CMAKE_SOURCE_DIR}/jni/*.cpp
        ${CMAKE_SOURCE_DIR}/srt/*.cpp
        ${CMAKE_SOURCE_DIR}/../../../../iconnection/*.cpp
        )

add_library( # Sets the name of the library.
        ${libname}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${src-files}
        )

#
set(native-libs
        log
        )


set(third-party-libs_srt
        srt
        ssl
        )

target_link_libraries( # Specifies the target library.
        ${libname}
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        ${third-party-libs_srt}
        ${native-libs}
        )