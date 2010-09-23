#!/bin/sh

###############################################################################
#                                                                             #
#                            Windows GCC Build                                #
#                                                                             #
# Builds gcc for this platform using mingw-w64.                               #
#                                                                             #
###############################################################################

TOP=$(dirname $0)
CURR_DIR=$( (cd "$TOP" && pwd) )

export PATH=/mingw32/bin:/bin:$PATH

#To get it to compile properly, you'll need to make several changes.
#
#packages
#
#sudo apt-get install bison flex

cd "$CURR_DIR"

#Build
echo Building GCC...
#Loop 3 times b/c tar will emit messages like "file changed as we read it" that although 
#they aren't a real problem, still cause tar to give an error return code. So we run 
#it again and the problem should be resolved.
i=0
while [ "$i" -lt "3" ]
do
    make -f "gccbuild-w64-x86_64.mk" \
        MAKEINFO=true 
    ((i++))
done

#Set flags
HOST_TRIPLET=x86_64-w64-mingw32
HOST_TRIPLET_DASH=${HOST_TRIPLET}-
STRIP="/mingw/bin/strip"
STRIP_NATIVE="$CURR_DIR/build/root/bin/strip"

#Strip
echo Stripping executables and shared libraries...
cd "$CURR_DIR"
$STRIP build/root/bin/*.exe
$STRIP_NATIVE build/root/bin/*.dll
$STRIP_NATIVE build/root/lib/bin/*.dll
$STRIP build/root/${HOST_TRIPLET}/bin/*.exe
$STRIP build/root/libexec/gcc/${HOST_TRIPLET}/4.5.2/*.exe

#Create copies
echo Creating symbolic links...
cd build/root/bin/
ln -s -f ${HOST_TRIPLET_DASH}addr2line addr2line
ln -s -f ${HOST_TRIPLET_DASH}ar ar
ln -s -f ${HOST_TRIPLET_DASH}as as
ln -s -f ${HOST_TRIPLET_DASH}c++ c++
ln -s -f ${HOST_TRIPLET_DASH}c++filt c++filt
ln -s -f ${HOST_TRIPLET_DASH}cpp cpp
ln -s -f ${HOST_TRIPLET_DASH}dlltool dlltool
ln -s -f ${HOST_TRIPLET_DASH}dllwrap dllwrap
ln -s -f ${HOST_TRIPLET_DASH}g++ g++
ln -s -f ${HOST_TRIPLET_DASH}gcc gcc
ln -s -f ${HOST_TRIPLET_DASH}gccbug gccbug
ln -s -f ${HOST_TRIPLET_DASH}gcov gcov
ln -s -f ${HOST_TRIPLET_DASH}gprof gprof
ln -s -f ${HOST_TRIPLET_DASH}ld ld
ln -s -f ${HOST_TRIPLET_DASH}nm nm
ln -s -f ${HOST_TRIPLET_DASH}objcopy objcopy
ln -s -f ${HOST_TRIPLET_DASH}objdump objdump
ln -s -f ${HOST_TRIPLET_DASH}ranlib ranlib
ln -s -f ${HOST_TRIPLET_DASH}readelf readelf
ln -s -f ${HOST_TRIPLET_DASH}size size
ln -s -f ${HOST_TRIPLET_DASH}strings strings
ln -s -f ${HOST_TRIPLET_DASH}strip strip
ln -s -f ${HOST_TRIPLET_DASH}windmc windmc
ln -s -f ${HOST_TRIPLET_DASH}windres windres
cd ../lib/gcc/${HOST_TRIPLET}/4.5.2/
ln -s -f ../lib64/libgcc_s.a libgcc_s.a

#Fix libtool files...
#Other projects may attempt to use the libtool files which point to directories that may or may not 
#exist on the system building the libraries. e.g. libstc++.la points to 
#/c/dev/build/root/lib/gcc/i686-w64-mingw32/4.5.2/ which existed when gcc was built, but is not longer 
#around after we've installed this gcc to D:\OSSBuild\Tools\msys-x86\.
echo Fixing libtool files...
cd "$CURR_DIR/build/root/lib/gcc/${HOST_TRIPLET}/4.5.2/"
rm -f libgfortran.la
rm -f libgfortranbegin.la
rm -f libobjc.la
rm -f libssp.la
rm -f libssp_nonshared.la
rm -f libstdc++.la
rm -f libsupc++.la
cd debug/
rm -f libstdc++.la

#Download OpenGL extension headers from the Khronos group
echo Adding additional OpenGL headers...
cd "$CURR_DIR/build/root/${HOST_TRIPLET}/include/GL/"
wget http://www.opengl.org/registry/api/glext.h
wget http://www.opengl.org/registry/api/glxext.h
wget http://www.opengl.org/registry/api/wglext.h
cd ../

#Create lzma bin archive
echo Creating lzma compressed archive...
cd "$CURR_DIR"
tar cfa mingw-w64-x86_64-ossbuild-bin.tar.lzma \
    -C build/root \
    --owner 0 --group 0 \
    --exclude=CVS --exclude=.svn --exclude=.*.marker \
	.

