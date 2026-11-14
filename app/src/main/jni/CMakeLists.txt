cmake_minimum_required(VERSION 3.22.1)
project("rena")

set(CMAKE_ANDROID_API_MIN 26)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(COMMON_C_FLAGS
    -Os
    -w
    -s
    -fdata-sections
    -ffunction-sections
    -fvisibility=hidden
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fomit-frame-pointer
    -fmerge-all-constants
    -fstack-protector-strong
    -fpermissive
    -fexceptions
    -Wno-error=format-security
)

set(COMMON_CXX_FLAGS
    ${COMMON_C_FLAGS}
    -fvisibility-inlines-hidden
    -std=c++17
    -Werror
    -Wno-error=c++11-narrowing
    -Wall
)

add_compile_options(
    $<$<COMPILE_LANGUAGE:C>:${COMMON_C_FLAGS}>
    $<$<COMPILE_LANGUAGE:CXX>:${COMMON_CXX_FLAGS}>
)

add_library(
    rena
    SHARED
    native-lib.cpp
    ridhoae303.cpp
)

target_compile_definitions(rena PRIVATE RENA_MIN_ANDROID_API=26)

target_include_directories(rena PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
    rena
    android
    dl
    EGL
    GLESv2
    log
)

target_link_options(rena PRIVATE
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--exclude-libs,ALL
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
    -Wl,-z,nodlopen
    -Wl,-z,nodump
    -Wl,-z,max-page-size=16384
)
