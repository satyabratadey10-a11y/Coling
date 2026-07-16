cmake_minimum_required(VERSION 3.22.1)

project("coling_native")

# Set C++ standard to C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Android Log library
find_library(log-lib log)

# Find OpenGL ES 3.x and EGL
find_library(gles-lib GLESv3)
find_library(egl-lib EGL)

# Source files
set(SOURCE_FILES
    jni/coling-jni.cpp
    core/color_engine.cpp
)

# Shared library target
add_library(coling_native SHARED ${SOURCE_FILES})

# Include directories
target_include_directories(coling_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/core
    ${CMAKE_CURRENT_SOURCE_DIR}/jni
)

# Link libraries
target_link_libraries(coling_native
    ${log-lib}
    ${gles-lib}
    ${egl-lib}
)
