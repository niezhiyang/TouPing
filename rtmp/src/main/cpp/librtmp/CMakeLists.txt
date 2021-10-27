#关闭ssl 不支持rtmps  rtmp   加密  传递一变量  进制 加密验证
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO"  )

# 只要是本文件 .c 文件 都要引入给 rtmp_source变量，把 这个编译成 rtmp.a
# 在的 CmakeLists 中可以 add_subdirectory(librtmp) 添加这个库，
file(GLOB rtmp_source  *.c)
add_library(rtmp
        STATIC
        ${rtmp_source})

