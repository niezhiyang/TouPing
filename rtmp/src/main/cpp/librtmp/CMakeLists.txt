# 关闭ssl 不支持 rtmps rtmp ,加密验证
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPIO")
# 设置遍历，或者 在  buid.gradle中

file(GLOB rtmp_source *.c)

# 编译一个 rtmp  的静态库
add_library(rtmp STATIC ${rtmp_source})