#!/bin/sh

###############################################################################
#                                                                             #
#                            Windows GCC Build                                #
#                                                                             #
# Builds gcc for this platform using mingw-w64.                               #
#                                                                             #
###############################################################################

TOP=$(dirname $0)/..
TOOLS_DIR=$TOP/Tools

export PATH=/mingw32/bin:/bin:$PATH

#Global flags
STRIP="/mingw/bin/strip"

#To get it to compile properly, you'll need to make several changes.
#
#packages
#
#sudo apt-get install bison flex

cd "$TOOLS_DIR"

#Build
echo Building GCC...
#Loop 3 times b/c tar will emit messages like "file changed as we read it" that although 
#they aren't a real problem, still cause tar to give an error return code. So we run 
#it again and the problem should be resolved.
i=0
while [ "$i" -lt "3" ]
do
    make -f "gccbuild-w64-x86.mk" \
        MAKEINFO=true \
        LD="ld -m i386pe" \
        RC="windres -F pe-i386" \
        WINDRES="windres -F pe-i386" \
        DLLTOOL="dlltool -f--32 -m i386 --export-all-symbols" \
        RCFLAGS="-F pe-i386" 
    ((i++))
done

#Strip
echo Stripping executables and shared libraries...
cd "$TOOLS_DIR"
$STRIP build/root/bin/*.exe
$STRIP build/root/bin/*.dll
$STRIP build/root/lib/bin/*.dll
$STRIP build/root/i686-w64-mingw32/bin/*.exe
$STRIP build/root/libexec/gcc/i686-w64-mingw32/4.5.2/*.exe

#Create lzma bin archive
echo Creating lzma compressed archive...
cd "$TOOLS_DIR"
tar cfa mingw-w64-x86-ossbuild-bin.tar.lzma \
    -C build/root \
    --owner 0 --group 0 \
    --exclude=CVS --exclude=.svn --exclude=.*.marker \
	.

