cmake_minimum_required(VERSION 3.22.1)

project("PocketCodeNative")

# Set 16KB page size alignment for Android compatibility
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Add the native library for C/C++ compilation
add_library(
    pocketcode-native
    SHARED
    native-lib.cpp
    c_compiler.cpp
    cpp_compiler.cpp
)

# Link against Android log library
find_library(log-lib log)

target_link_libraries(
    pocketcode-native
    ${log-lib}
)

# Set C++ standard
set_property(TARGET pocketcode-native PROPERTY CXX_STANDARD 17)

# Ensure 16KB alignment for the shared library
set_target_properties(pocketcode-native PROPERTIES
    LINK_FLAGS "-Wl,-z,max-page-size=16384"
)