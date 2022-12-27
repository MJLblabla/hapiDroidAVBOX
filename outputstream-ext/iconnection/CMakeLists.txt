cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
# Declares and names the project.

project("iconnection")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(jnilibs ${CMAKE_SOURCE_DIR}/../jniLibs)
set(libname iconnection)

add_library( # Sets the name of the library.
        ${libname}
        # Sets the library as a shared library.
        SHARED
        IConnection.cpp IConnection.h
        IConnectionJNICaller.cpp IConnectionJNICaller.h
        BlockQueue.h
        LogUtil.h
        )

#
set(native-libs
        log
        )

target_link_libraries( # Specifies the target library.
        ${libname}
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        ${native-libs}
        )