cmake_minimum_required(VERSION 3.4.1)
# 添加 rtmp 库，这个定义是在 rtmp 中的 CmakeList中
add_subdirectory(librtmp)
add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp )
find_library( # Sets the name of the path variable.
        log-lib
        log )
# 把 rtmp 库 链接到 native-lib 中
target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        rtmp)