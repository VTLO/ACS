cmake_minimum_required(VERSION 3.22.1)
project(vtlo_hidstub C)

add_library(vtlo_hidstub SHARED hidapi_stub.c)
set_target_properties(vtlo_hidstub PROPERTIES C_VISIBILITY_PRESET hidden)
# 16 KB page alignment, like the FFmpegKit libraries.
target_link_options(vtlo_hidstub PRIVATE "-Wl,-z,max-page-size=16384")
