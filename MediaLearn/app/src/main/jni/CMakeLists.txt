cmake_minimum_required(VERSION 3.4.1)

project(native-lib)

message("cmake start...")

#include_directories(lib_rtmp/include)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

#add_library(rtmp STATIC IMPORTED)
#set_target_properties(rtmp PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/lib_rtmp/libs/${CMAKE_ANDROID_ARCH_ABI}/librtmp.a)



include_directories(
        egl
        utils
        native_looper
        lib_rtmp
        test_livepush
)

file(GLOB SRC_LIST "*.cpp" "egl/*.cpp" "lib_rtmp/*.c" "test_livepush/*.cpp" "test_livepush/*.c")

add_library(native SHARED ${SRC_LIST})

target_link_libraries(
        native
        -landroid
        EGL
        GLESv3
        log
)



#target_include_directories(native SYSTEM PUBLIC ${CMAKE_SOURCE_DIR}/lib_rtmp/include)

message("cmake end")