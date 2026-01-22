cmake_minimum_required(VERSION 3.22)
project(argon2jni LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_EXTENSIONS OFF)

set(ARGON2_DIR ${CMAKE_CURRENT_LIST_DIR}/../../phc-winner-argon2)

add_library(argon2 SHARED
        ${CMAKE_CURRENT_LIST_DIR}/org_signal_argon2_Argon2Native.c
        ${ARGON2_DIR}/src/blake2/blake2b.c
        ${ARGON2_DIR}/src/argon2.c
        ${ARGON2_DIR}/src/core.c
        ${ARGON2_DIR}/src/encoding.c
        ${ARGON2_DIR}/src/genkat.c
        ${ARGON2_DIR}/src/ref.c
        ${ARGON2_DIR}/src/thread.c
)

target_include_directories(argon2 PRIVATE
        ${ARGON2_DIR}/include
)

# LOCAL_CFLAGS += -Wall
target_compile_options(argon2 PRIVATE -Wall)
