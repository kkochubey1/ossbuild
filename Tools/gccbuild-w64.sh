#!/bin/sh

###############################################################################
#                                                                             #
#                            Windows GCC Build                                #
#                                                                             #
# Builds gcc for this platform using mingw-w64.                               #
#                                                                             #
###############################################################################

TOP=$(dirname $0)

#Global flags
CFLAGS="-m32 -D_WIN32_WINNT=0x0501 -Dsocklen_t=int "
CPPFLAGS="-DMINGW64 -D__MINGW64__ -DMINGW32 -D__MINGW32__"
LDFLAGS="-Wl,--enable-auto-image-base -Wl,--enable-auto-import -Wl,--enable-runtime-pseudo-reloc-v2 -Wl,--kill-at "
CXXFLAGS="${CFLAGS}"

#To get it to compile properly, you'll need to make several changes.
#
#packages
#
#sudo apt-get install bison flex
#
#binutils
#
#Any file that errors out ending in .texinfo needs to have 
#@sysindex or related fields removed (see the error text for the 
#offending line). You can just delete the whole line. 
#Known files where this may be an issue are:
#    build/binutils/src/bfd/doc/bfd.texinfo         (line 7)
#    build/binutils/src/binutils/doc/binutils.texi  (line 5)
#    build/binutils/src/etc/doc/standards.texi      (lines 18-21)
#    build/binutils/src/ld/doc/ld.texinfo           (lines 6, 4089, 4093)
#    build/binutils/src/gas/doc/as.texinfo          (line 100)
#
#gmp
#
#You'll need to edit build/gcc/src/gmp/configure and put in an ms-style path for 
#the include where mp_limb_t is being tested. e.g.:
#    #include \"$srcdir/gmp-h.in\" needs to be #include \"E:\\Work\\OSSBuild\\Tools\\build\\gcc\\src\\gmp\\gmp-h.in\"

cd "$TOP"
make -f "gccbuild-w64-x86.mk" MAKEINFO=true
