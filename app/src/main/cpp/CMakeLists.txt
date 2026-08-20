cmake_minimum_required(VERSION 3.22.1)
project(zekatr_llama)

set(CMAKE_CXX_STANDARD 17)

# llama.cpp submodule (bkz. README.md - git submodule add ile eklenmesi gerekir)
add_subdirectory(llama.cpp)

add_library(zekatr_llama SHARED
    llama_bridge.cpp
)

target_include_directories(zekatr_llama PRIVATE
    llama.cpp/include
    llama.cpp/ggml/include
)

target_link_libraries(zekatr_llama
    llama
    ggml
    android
    log
)
