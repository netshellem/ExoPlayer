# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)
# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Werror")
set(jni_headers_PATH src/main/jni/jni_include)
set(static_DIR src/main/jni/static)
include_directories(
    ${jni_headers_PATH}
    )


add_library( # Specifies the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/jni/drm_jni.cpp
             src/main/jni/drm.cpp
             src/main/jni/spi.cpp
              )
add_library( libdrm STATIC IMPORTED )
set_target_properties(libdrm PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/src/main/jni/static/${ANDROID_ABI}/libirdetodrm.a)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_link_libraries( native-lib libdrm  ${log-lib} )

