### FFmpeg 编译过程



> 环境准备
>
> https://ffmpeg.org/releases/ffmpeg-4.2.1.tar.bz2
>
>  https://dl.google.com/android/repository/android-ndk-r20-linux-x86_64.zip

#### 1 准备资料

1.准备环境（linux，mac）建议不要在windows上编译，很麻烦，还需要去搭建环境，可以安装虚拟机编译。以下选择用mac系统进行编译。 2.下载ffmpeg源码，我编译的是4.2.1版本的[ffmpeg](https://ffmpeg.org/download.html)

2.下载ndk，建议下载最新版的[ndk](https://developer.android.com/ndk/downloads?hl=zh-cn)   本文使用的r20c版本



#### 2 ndk 环境配置

 打开终端使用命令：vim /etc/profile  即可打开环境变量配置，到这一步应该没问题。 





#### 3 编写脚本文件

脚本的作用就是把ffmpeg源码打包成我们需要的.so文件，供我们的Android项目调用。

新建一个在ffmpeg文件夹下新建android_build.sh文件。

```
#!/bin/bash
export NDK=/root/ndk/android-ndk-r20
# 当前系统
export HOST_TAG=linux-x86_64
# 支持的 Android CUP 架构
# export ARCH=aarch64
# export CPU=armv8-a
export ARCH=armv7a
export CPU=armv7-a
# 支持的 Android 最低系统版本
export MIN=21
export ANDROID_NDK_PLATFORM=android-21

export PREFIX=$(pwd)/android/$CPU

export MIN_PLATFORM=$NDK/platforms/android-$MIN
export SYSROOT=$NDK/sysroot
export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/$HOST_TAG
export AR=$TOOLCHAIN/bin/arm-linux-androideabi-ar
export AS=$TOOLCHAIN/bin/arm-linux-androideabi-as
export CC=$TOOLCHAIN/bin/$ARCH-linux-androideabi$MIN-clang
echo "-----------------------------"
echo $CC
export CXX=$TOOLCHAIN/bin/$ARCH-linux-androideabi$MIN-clang++
export LD=$TOOLCHAIN/bin/arm-linux-androideabi-ld
export NM=$TOOLCHAIN/bin/arm-linux-androideabi-nm
export RANLIB=$TOOLCHAIN/bin/arm-linux-androideabi-ranlib
export STRIP=$TOOLCHAIN/bin/arm-linux-androideabi-strip

OPTIMIZE_CFLAGS="-DANDROID -I$NDK/sysroot/usr/include/arm-linux-androideabi/"
ADDI_LDFLAGS="-Wl,-rpath-link=$MIN_PLATFORM/arch-arm/usr/lib -L$MIN_PLATFORM/arch-arm/usr/lib -nostdlib"

sed  -i "" "s/SLIBNAME_WITH_MAJOR='\$(SLIBNAME).\$(LIBMAJOR)'/SLIBNAME_WITH_MAJOR='\$(SLIBPREF)\$(FULLNAME)-\$(LIBMAJOR)\$(SLIBSUF)'/" configure
sed  -i "" "s/LIB_INSTALL_EXTRA_CMD='\$\$(RANLIB) \"\$(LIBDIR)\\/\$(LIBNAME)\"'/LIB_INSTALL_EXTRA_CMD='\$\$(RANLIB) \"\$(LIBDIR)\\/\$(LIBNAME)\"'/" configure
sed  -i "" "s/SLIB_INSTALL_NAME='\$(SLIBNAME_WITH_VERSION)'/SLIB_INSTALL_NAME='\$(SLIBNAME_WITH_MAJOR)'/" configure
sed  -i "" "s/SLIB_INSTALL_LINKS='\$(SLIBNAME_WITH_MAJOR) \$(SLIBNAME)'/SLIB_INSTALL_LINKS='\$(SLIBNAME)'/" configure
sed -i -e 's/#define getenv(x) NULL/\/\*#define getenv(x) NULL\*\//g' config.h
# sed  -i "" "s/SHFLAGS='-shared -Wl,-soname,\$(SLIBNAME)'/SHFLAGS='-shared -soname \$(SLIBNAME)'/" configure
# sed  -i "" "s/-Wl//g" configure

./configure \
--prefix=$PREFIX \
--ar=$AR \
--as=$AS \
--cc=$CC \
--cxx=$CXX \
--nm=$NM \
--ranlib=$RANLIB \
--strip=$STRIP \
--arch=$ARCH \
--target-os=android \
--enable-cross-compile \
--disable-asm \
--enable-shared \
--disable-static \
--disable-ffprobe \
--disable-ffplay \
--disable-ffmpeg \
--disable-debug \
--disable-symver \
--disable-stripping \
--extra-cflags="-Os -fpic $OPTIMIZE_CFLAGS"\
--extra-ldflags="$ADDI_LDFLAGS"

sed  -i "" "s/#define HAVE_TRUNC 0/#define HAVE_TRUNC 1/" config.h
sed  -i "" "s/#define HAVE_TRUNCF 0/#define HAVE_TRUNCF 1/" config.h
sed  -i "" "s/#define HAVE_RINT 0/#define HAVE_RINT 1/" config.h
sed  -i "" "s/#define HAVE_LRINT 0/#define HAVE_LRINT 1/" config.h
sed  -i "" "s/#define HAVE_LRINTF 0/#define HAVE_LRINTF 1/" config.h
sed  -i "" "s/#define HAVE_ROUND 0/#define HAVE_ROUND 1/" config.h
sed  -i "" "s/#define HAVE_ROUNDF 0/#define HAVE_ROUNDF 1/" config.h
sed  -i "" "s/#define HAVE_CBRT 0/#define HAVE_CBRT 1/" config.h
sed  -i "" "s/#define HAVE_CBRTF 0/#define HAVE_CBRTF 1/" config.h
sed  -i "" "s/#define HAVE_COPYSIGN 0/#define HAVE_COPYSIGN 1/" config.h
sed  -i "" "s/#define HAVE_ERF 0/#define HAVE_ERF 1/" config.h
sed  -i "" "s/#define HAVE_HYPOT 0/#define HAVE_HYPOT 1/" config.h
sed  -i "" "s/#define HAVE_ISNAN 0/#define HAVE_ISNAN 1/" config.h
sed  -i "" "s/#define HAVE_ISFINITE 0/#define HAVE_ISFINITE 1/" config.h
sed  -i "" "s/#define HAVE_INET_ATON 0/#define HAVE_INET_ATON 1/" config.h
sed  -i "" "s/#define getenv(x) NULL/\\/\\/ #define getenv(x) NULL/" config.h

```

复制上面的脚本代码到我们新建的文件中，打开终端，到ffmpeg文件下使用命令就可以进行编译 

sh build.sh