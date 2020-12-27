cmake_minimum_required(VERSION 3.4.1)

project(native-lib)

message("cmake start...")


include_directories(
        egl
        utils
        native_looper
)

file(GLOB SRC_LIST "*.cpp" "egl/*.cpp" "native_looper/*.cpp")

add_library(native SHARED ${SRC_LIST})


target_link_libraries(
        native
        EGL
        GLESv3
        log
)

message("cmake end")