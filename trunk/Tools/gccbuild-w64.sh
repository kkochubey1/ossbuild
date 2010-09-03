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

export PATH=/bin:$PATH

#Global flags
STRIP="/mingw/bin/strip"

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

cd "$TOOLS_DIR"

#Build
make -f "gccbuild-w64-x86.mk" \
    MAKEINFO=true \
    LD="ld -m i386pe" \
    RC="windres -F pe-i386" \
    WINDRES="windres -F pe-i386" \
    DLLTOOL="dlltool -f--32 -m i386 --export-all-symbols" \
    RCFLAGS="-F pe-i386" 

