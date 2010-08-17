@echo off

REM **********************************************************************
REM * Creates a suitable MSys/MinGW environment for building the Windows 
REM * libraries using GCC and associated auto tools.
REM **********************************************************************

set DOWNLOAD=1
set UNTAR=1
set CLEAN=1

set DIR=%~dp0.
set CURRDIR=%CD%
set TOPDIR=%DIR%\..
set TMPDIR=%DIR%\tmp
set TOOLSDIR=%DIR%
set PATCHESDIR=%TOOLSDIR%\patches
set MSYSDIR=%TOOLSDIR%\msys
set MINGWDIR=%MSYSDIR%\mingw
set DEST_MSYS_DIR=%MSYSDIR%
set MINGW_GET_DEFAULTS=%TMPDIR%\var\lib\mingw-get\data\defaults.xml

rem set GCCDIR=cross-mingw.gcc444.generic.20100612
rem set GCC=http://komisar.gin.by/mingw/cross-mingw.gcc444.generic.20100612.7z
rem set GCCDIR=cross-mingw.gcc450.generic.20100612
rem set GCC=http://komisar.gin.by/mingw/cross-mingw.gcc450.generic.20100612.7z
rem set GCCDIR=cross-mingw.gcc451.generic.20100615
rem set GCC=http://komisar.gin.by/mingw/cross-mingw.gcc451.generic.20100615.7z
rem set GCCDIR=cross-mingw.gcc451.generic.20100615.1
rem set GCC=http://komisar.gin.by/mingw/cross-mingw.gcc451.generic.20100615.1.7z

rem set PKG_MINGW_W64_GCC_X86_BIN=http://downloads.sourceforge.net/project/mingw-w64/Toolchains targetting Win32/Automated Builds/mingw-w32-1.0-bin_i686-mingw_20100618.zip?use_mirror=iweb
rem set PKG_MINGW_W64_GCC_X86_64_BIN=http://downloads.sourceforge.net/project/mingw-w64/Toolchains targetting Win64/Automated Builds/mingw-w64-bin_i686-mingw_20100619.zip?use_mirror=iweb

set PKG_TDM_GCC_MINGW_CORE_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm-1 SJLJ/gcc-4.5.0-tdm-1-core.tar.lzma?use_mirror=voxel
set PKG_TDM_GCC_MINGW_BINUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GNU-Binutils/binutils-2.20.1/binutils-2.20.1-2-mingw32-bin.tar.gz?use_mirror=voxel
set PKG_TDM_GCC_MINGW_MINGWRT_DEV=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/MinGW-RT/mingwrt-3.18/mingwrt-3.18-mingw32-dev.tar.gz?use_mirror=voxel
set PKG_TDM_GCC_MINGW_MINGWRT_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/MinGW-RT/mingwrt-3.18/mingwrt-3.18-mingw32-dll.tar.gz?use_mirror=voxel
set PKG_TDM_GCC_MINGW_W32API_DEV=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/Win32-API/w32api-3.14/w32api-3.14-mingw32-dev.tar.gz?use_mirror=iweb
set PKG_TDM_GCC_MINGW_CPP_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm-1 SJLJ/gcc-4.5.0-tdm-1-c++.tar.lzma?use_mirror=softlayer
set PKG_TDM_GCC_MINGW_OBJC_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm-1 SJLJ/gcc-4.5.0-tdm-1-objc.tar.lzma?use_mirror=voxel

set PKG_TDM_GCC_MINGW64_CORE_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm64-1/gcc-4.5.0-tdm64-1-core.tar.lzma?use_mirror=voxel
set PKG_TDM_GCC_MINGW64_BINUTILS_BIN=http://downloads.sourceforge.net/project/tdm-gcc/GNU binutils/binutils-2.20.51-tdm64-20100322.tar.lzma?use_mirror=voxel
set PKG_TDM_GCC_MINGW64_MINGWRT_RUNTIME=http://downloads.sourceforge.net/project/tdm-gcc/MinGW-w64 runtime/GCC 4.5 series/mingw64-runtime-tdm64-gcc45-svn2455.tar.lzma?use_mirror=voxel
set PKG_TDM_GCC_MINGW64_CPP_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm64-1/gcc-4.5.0-tdm64-1-c++.tar.lzma?use_mirror=voxel
set PKG_TDM_GCC_MINGW64_OBJC_BIN=http://downloads.sourceforge.net/project/tdm-gcc/TDM-GCC 4.5 series/4.5.0-tdm64-1/gcc-4.5.0-tdm64-1-objc.tar.lzma?use_mirror=voxel

set PKG_MSYSGIT_BIN=http://msysgit.googlecode.com/files/PortableGit-1.7.1-preview20100612.7z

set PKG_ACTIVESTATE_PERL_DIR=ActivePerl-5.10.1.1007-MSWin32-x86-291969
set PKG_ACTIVESTATE_PERL_BIN=http://downloads.activestate.com/ActivePerl/releases/5.10.1.1007/ActivePerl-5.10.1.1007-MSWin32-x86-291969.zip
set PKG_STRAWBERRY_PERL_BIN=http://strawberryperl.com/download/strawberry-perl-5.12.0.1.zip

set MINGW_GET=http://downloads.sourceforge.net/project/mingw/Automated MinGW Installer/mingw-get/mingw-get-0.1-mingw32-alpha-2-bin.tar.gz?use_mirror=softlayer

set PKG_MSYS_CORE_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/msys-core/msys-1.0.15-1/msysCORE-1.0.15-1-msys-1.0.15-dev.tar.lzma?use_mirror=iweb
set PKG_MSYS_CORE_EXT=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/msys-core/msys-1.0.15-1/msysCORE-1.0.15-1-msys-1.0.15-ext.tar.lzma?use_mirror=iweb
set PKG_MSYS_RXVT_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/rxvt/rxvt-2.7.2-3/rxvt-2.7.2-3-msys-1.0.14-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_BASH_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/bash/bash-3.1.17-3/bash-3.1.17-3-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_REGEX_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/regex/regex-1.20090805-2/libregex-1.20090805-2-msys-1.0.13-dll-1.tar.lzma?use_mirror=iweb
set PKG_MSYS_TERMCAP_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/termcap/termcap-0.20050421_1-2/libtermcap-0.20050421_1-2-msys-1.0.13-dll-0.tar.lzma?use_mirror=iweb
set PKG_MSYS_COREUTILS_EXT=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/coreutils/coreutils-5.97-3/coreutils-5.97-3-msys-1.0.13-ext.tar.lzma?use_mirror=iweb
set PKG_MSYS_COREUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/coreutils/coreutils-5.97-3/coreutils-5.97-3-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_ICONV_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/libiconv/libiconv-1.13.1-2/libiconv-1.13.1-2-msys-1.0.13-dll-2.tar.lzma?use_mirror=iweb
set PKG_MSYS_ICONV_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/libiconv/libiconv-1.13.1-2/libiconv-1.13.1-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_CHARSET_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/libiconv/libiconv-1.13.1-2/libcharset-1.13.1-2-msys-1.0.13-dll-1.tar.lzma?use_mirror=iweb
set PKG_MSYS_ZLIB_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/zlib/zlib-1.2.3-2/zlib-1.2.3-2-msys-1.0.13-dll.tar.lzma?use_mirror=voxel
set PKG_MSYS_INTL_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/gettext/gettext-0.17-2/libintl-0.17-2-msys-dll-8.tar.lzma?use_mirror=iweb
set PKG_MSYS_GETTEXT_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/gettext/gettext-0.17-2/libgettextpo-0.17-2-msys-dll-0.tar.lzma?use_mirror=iweb
set PKG_MSYS_ASPRINTF_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/gettext/gettext-0.17-2/libasprintf-0.17-2-msys-dll-0.tar.lzma?use_mirror=iweb
set PKG_MSYS_GETTEXT_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/gettext/gettext-0.17-2/gettext-0.17-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_WGET_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/wget/wget-1.12-1/wget-1.12-1-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_UNZIP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/unzip/unzip-6.0-1/unzip-6.0-1-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_ZIP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/zip/zip-3.0-1/zip-3.0-1-msys-1.0.14-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_OPTS_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/autogen/autogen-5.10.1-1/libopts-5.10.1-1-msys-1.0.15-dll-25.tar.lzma?use_mirror=iweb
set PKG_MSYS_AUTOGEN_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/autogen/autogen-5.10.1-1/autogen-5.10.1-1-msys-1.0.15-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_POPT_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/popt/popt-1.15-2/libpopt-1.15-2-msys-1.0.13-dll-0.tar.lzma?use_mirror=voxel
set PKG_MSYS_CYGUTILS_DOS2UNIX_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/cygutils/cygutils-1.3.4-4/cygutils-dos2unix-1.3.4-4-msys-1.0.13-bin.tar.lzma?use_mirror=softlayer
set PKG_MSYS_CYGUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/cygutils/cygutils-1.3.4-4/cygutils-1.3.4-4-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_UNZIP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/unzip/unzip-6.0-1/unzip-6.0-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_PATCH_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/patch/patch-2.6.1-1/patch-2.6.1-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_DIFFUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/diffutils/diffutils-2.8.7.20071206cvs-3/diffutils-2.8.7.20071206cvs-3-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_GREP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/grep/grep-2.5.4-2/grep-2.5.4-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_TAR_EXT=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/tar/tar-1.23-1/tar-1.23-1-msys-1.0.13-ext.tar.lzma?use_mirror=iweb
set PKG_MSYS_TAR_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/tar/tar-1.23-1/tar-1.23-1-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_LESS_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/less/less-436-2/less-436-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_GZIP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/gzip/gzip-1.3.12-2/gzip-1.3.12-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_M4_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/m4/m4-1.4.14-1/m4-1.4.14-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_SED_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/sed/sed-4.2.1-2/sed-4.2.1-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_BISON_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/bison/bison-2.4.2-1/bison-2.4.2-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_FLEX_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/flex/flex-2.5.35-2/flex-2.5.35-2-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_PERL_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/perl/perl-5.6.1_2-2/perl-5.6.1_2-2-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_REBASE_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/rebase/rebase-3.0.1_1-2/rebase-3.0.1_1-2-msys-1.0.15-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_CRYPT_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/crypt/crypt-1.1_1-3/libcrypt-1.1_1-3-msys-1.0.13-dll-0.tar.lzma?use_mirror=softlayer
set PKG_MSYS_CRYPT_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/crypt/crypt-1.1_1-3/crypt-1.1_1-3-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_BZIP2_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/bzip2/bzip2-1.0.5-2/libbz2-1.0.5-2-msys-1.0.13-dll-1.tar.lzma?use_mirror=iweb
set PKG_MSYS_BZIP2_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/bzip2/bzip2-1.0.5-2/bzip2-1.0.5-2-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_OPENSSL_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/openssl/openssl-1.0.0-1/libopenssl-1.0.0-1-msys-1.0.13-dll-100.tar.lzma?use_mirror=voxel
set PKG_MSYS_OPENSSL_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/openssl/openssl-1.0.0-1/openssl-1.0.0-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_GMP_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/gmp/gmp-5.0.1-1/libgmp-5.0.1-1-msys-1.0.13-dll-10.tar.lzma?use_mirror=softlayer
set PKG_MSYS_GUILE_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/guile/guile-1.8.7-2/libguile-1.8.7-2-msys-1.0.15-dll-17.tar.lzma?use_mirror=voxel
set PKG_MSYS_GUILE_RTM=http://downloads.sourceforge.net/project/mingw/MSYS/guile/guile-1.8.7-2/libguile-1.8.7-2-msys-1.0.15-rtm.tar.lzma?use_mirror=voxel
set PKG_MSYS_GUILE_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/guile/guile-1.8.7-2/guile-1.8.7-2-msys-1.0.15-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_LTDL_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/libtool/libtool-2.2.7a-2/libltdl-2.2.7a-2-msys-1.0.13-dll-7.tar.lzma?use_mirror=voxel
set PKG_MSYS_XML2_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/libxml2/libxml2-2.7.6-1/libxml2-2.7.6-1-msys-1.0.13-dll-2.tar.lzma?use_mirror=voxel
set PKG_MSYS_XML2_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/libxml2/libxml2-2.7.6-1/libxml2-2.7.6-1-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_MAKE_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/make/make-3.81-3/make-3.81-3-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_LOCATE_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/findutils/findutils-4.4.2-2/locate-4.4.2-2-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_FINDUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/findutils/findutils-4.4.2-2/findutils-4.4.2-2-msys-1.0.13-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_MKTEMP_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/mktemp/mktemp-1.6-2/mktemp-1.6-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_GAWK_BIN=http://downloads.sourceforge.net/project/mingw/MSYS/gawk/gawk-3.1.7-2/gawk-3.1.7-2-msys-1.0.13-bin.tar.lzma?use_mirror=iweb
set PKG_MSYS_LIBGCC_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libgcc-4.5.0-1-mingw32-dll-1.tar.lzma?use_mirror=iweb
set PKG_MSYS_FLIP_BIN=https://ccrma.stanford.edu/~craig/utility/flip/flip.exe

set PKG_MINGW_BASICBSDTAR_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/Utilities/basic bsdtar/basic-bsdtar-2.8.3-1/basic-bsdtar-2.8.3-1-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_AUTOCONF_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/autoconf/autoconf2.5/autoconf2.5-2.64-1/autoconf2.5-2.64-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_AUTOCONFWRAPPER_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/autoconf/wrapper/autoconf-7-1/autoconf-7-1-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_AUTOMAKE_1_4_BIN=http://sourceforge.net/projects/mingw/files/MinGW/automake/automake1.4/automake1.4-1.4p6-1/automake1.4-1.4p6-1-mingw32-bin.tar.lzma/download
set PKG_MINGW_AUTOMAKE_1_8_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/automake/automake1.8/automake1.8-1.8.5-1/automake1.8-1.8.5-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_AUTOMAKE_1_10_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/automake/automake1.10/automake1.10-1.10.2-1/automake1.10-1.10.2-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_AUTOMAKE_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/automake/automake1.11/automake1.11-1.11-1/automake1.11-1.11-1-mingw32-bin.tar.lzma?use_mirror=softlayer
set PKG_MINGW_AUTOMAKEWRAPPER_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/automake/wrapper/automake-4-1/automake-4-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_LTDL_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/libtool/libtool-2.2.7a-1/libltdl-2.2.7a-1-mingw32-dll-7.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBTOOL_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/libtool/libtool-2.2.7a-1/libtool-2.2.7a-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_PEXPORTS_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/pexports/pexports-0.44-1/pexports-0.44-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_GETTEXT_DEV=http://downloads.sourceforge.net/project/mingw/MinGW/gettext/gettext-0.17-1/gettext-0.17-1-mingw32-dev.tar.lzma?use_mirror=iweb
set PKG_MINGW_LIBGETTEXTPO_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/gettext/gettext-0.17-1/libgettextpo-0.17-1-mingw32-dll-0.tar.lzma?use_mirror=iweb
set PKG_MINGW_INTL_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/gettext/gettext-0.17-1/libintl-0.17-1-mingw32-dll-8.tar.lzma?use_mirror=iweb
set PKG_MINGW_LIBASPRINTF_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/gettext/gettext-0.17-1/libasprintf-0.17-1-mingw32-dll-0.tar.lzma?use_mirror=softlayer
set PKG_MINGW_GETTEXT_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/gettext/gettext-0.17-1/gettext-0.17-1-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_LIBCHARSET_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/libiconv/libiconv-1.13.1-1/libcharset-1.13.1-1-mingw32-dll-1.tar.lzma?use_mirror=iweb
set PKG_MINGW_LIBICONV_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/libiconv/libiconv-1.13.1-1/libiconv-1.13.1-1-mingw32-dll-2.tar.lzma?use_mirror=iweb
set PKG_MINGW_LIBICONV_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/libiconv/libiconv-1.13.1-1/libiconv-1.13.1-1-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_MAKE_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/make/make-3.82-mingw32/make-3.82-2-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_GCC_CORE_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/gcc-core-4.5.0-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_GCC_CPP_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/gcc-c++-4.5.0-1-mingw32-bin.tar.lzma?use_mirror=iweb
set PKG_MINGW_GCC_OBJC_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/gcc-objc-4.5.0-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBGNAT_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libgnat-4.5.0-1-mingw32-dll-4.5.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBGOMP_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libgomp-4.5.0-1-mingw32-dll-1.tar.lzma?use_mirror=softlayer
set PKG_MINGW_LIBGCC_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libgcc-4.5.0-1-mingw32-dll-1.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBSTDCPP_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libstdc++-4.5.0-1-mingw32-dll-6.tar.lzma?use_mirror=softlayer
set PKG_MINGW_LIBOBJC_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libobjc-4.5.0-1-mingw32-dll-2.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBSSP_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/gcc-4.5.0-1/libssp-4.5.0-1-mingw32-dll-0.tar.lzma?use_mirror=voxel
set PKG_MINGW_BINUTILS_BIN=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/GNU-Binutils/binutils-2.20.51/binutils-2.20.51-1-mingw32-bin.tar.lzma?use_mirror=voxel
set PKG_MINGW_W32API_DEV=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/Win32-API/w32api-3.14/w32api-3.14-mingw32-dev.tar.gz?use_mirror=softlayer
set PKG_MINGW_LIBGMP_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/gmp/gmp-5.0.1-1/libgmp-5.0.1-1-mingw32-dll-10.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBMPFR_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/mpfr/mpfr-2.4.1-1/libmpfr-2.4.1-1-mingw32-dll-1.tar.lzma?use_mirror=voxel
set PKG_MINGW_PTHREADS_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/pthreads-w32/pthreads-w32-2.8.0-3/libpthread-2.8.0-3-mingw32-dll-2.tar.lzma?use_mirror=voxel
set PKG_MINGW_LIBMPC_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/mpc/mpc-0.8.1-1/libmpc-0.8.1-1-mingw32-dll-2.tar.lzma?use_mirror=softlayer
set PKG_MINGW_MINGWRT_DLL=http://downloads.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/MinGW-RT/mingwrt-3.18/mingwrt-3.18-mingw32-dll.tar.gz?use_mirror=voxel
set PKG_MINGW_GLIB_DLL=http://ftp.gnome.org/pub/gnome/binaries/win32/glib/2.24/glib_2.24.0-2_win32.zip
set PKG_MINGW_PKGCONFIG_BIN=http://ftp.gnome.org/pub/gnome/binaries/win32/dependencies/pkg-config_0.23-3_win32.zip

set PATH=%TMPDIR%\bin;%TOOLSDIR%;%PATH%

mkdir "%TMPDIR%"
cd /d "%TMPDIR%"

rem Clean existing msys dir if we have one
if exist "%DEST_MSYS_DIR%" rmdir /S /Q "%DEST_MSYS_DIR%"
mkdir "%DEST_MSYS_DIR%"

mkdir "%MSYSDIR%"
mkdir "%MINGWDIR%"

rem Initialize mingw-get
rem For now we're not really using it, but perhaps in the future we will

if "%DOWNLOAD%" == "1" (
	cd /d "%TMPDIR%"
	wget --no-check-certificate -O mingw-get.tar.gz "%MINGW_GET%"
	cd "%MSYSDIR%"
)

if "%UNTAR%" == "1" (
	cd /d "%TMPDIR%"
	7za -y x mingw-get.tar.gz
	7za -y x mingw-get.tar
	del mingw-get.tar
	del mingw-get.tar.gz
	cd /d "%MSYSDIR%"
)

rem Outputs the following:
rem 
rem <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
rem <profile project="MinGW" application="mingw-get">
rem     <repository uri="http://prdownloads.sourceforge.net/mingw/%F.xml.lzma?download">
rem     </repository>
rem     <system-map id="default">
rem     <sysroot subsystem="mingw32" path="c:/MinGW" />
rem         <sysroot subsystem="MSYS" path="c:/MSYS/1.0" />
rem     </system-map>
rem </profile>
echo %MINGW_GET_DEFAULTS%
echo ^<?xml version="1.0" encoding="UTF-8" standalone="yes"?^>                                              > %MINGW_GET_DEFAULTS%
echo ^<!-- --^>    ^<profile project="MinGW" application="mingw-get"^>                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<repository uri="http://prdownloads.sourceforge.net/mingw/%%F.xml.lzma?download"^> >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^</repository^>                                                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<system-map id="default"^>                                                         >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<sysroot subsystem="mingw32" path="%MINGWDIR%" /^>                                 >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>            ^<sysroot subsystem="MSYS" path="%MSYSDIR%" /^>                                 >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^</system-map^>                                                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>    ^</profile^>                                                                            >> %MINGW_GET_DEFAULTS%

rem mingw-get install gcc

cd /d "%MSYSDIR%"
if "%DOWNLOAD%" == "1" (
	cd /d "%TMPDIR%"
	
	REM call :download mingw-gcc-bin %GCC%
	
	call :download msys-core-bin "%PKG_MSYS_CORE_BIN%"
	call :download msys-core-ext "%PKG_MSYS_CORE_EXT%"
	call :download msys-rxvt-bin "%PKG_MSYS_RXVT_BIN%"
	call :download msys-bash-bin "%PKG_MSYS_BASH_BIN%"
	call :download msys-regex-dll "%PKG_MSYS_REGEX_DLL%"
	call :download msys-termcap-dll "%PKG_MSYS_TERMCAP_DLL%"
	call :download msys-core-utils-ext "%PKG_MSYS_COREUTILS_EXT%"
	call :download msys-core-utils-bin "%PKG_MSYS_COREUTILS_BIN%"
	call :download msys-iconv-dll "%PKG_MSYS_ICONV_DLL%"
	call :download msys-iconv-bin "%PKG_MSYS_ICONV_BIN%"
	call :download msys-charset-dll "%PKG_MSYS_CHARSET_DLL%"
	call :download msys-zlib-dll "%PKG_MSYS_ZLIB_DLL%"
	call :download msys-intl-dll "%PKG_MSYS_INTL_DLL%"
	call :download msys-gettext-dll "%PKG_MSYS_GETTEXT_DLL%"
	call :download msys-asprintf-dll "%PKG_MSYS_ASPRINTF_DLL%"
	call :download msys-gettext-bin "%PKG_MSYS_GETTEXT_BIN%"
	call :download msys-wget-bin "%PKG_MSYS_WGET_BIN%"
	call :download msys-unzip-bin "%PKG_MSYS_UNZIP_BIN%"
	call :download msys-zip-bin "%PKG_MSYS_ZIP_BIN%"
	call :download msys-opts-dll "%PKG_MSYS_OPTS_DLL%"
	call :download msys-autogen-bin "%PKG_MSYS_AUTOGEN_BIN%"
	call :download msys-popt-bin "%PKG_MSYS_POPT_DLL%"
	call :download msys-cygutils-dos2unix-bin "%PKG_MSYS_CYGUTILS_DOS2UNIX_BIN%"
	call :download msys-cygutils-bin "%PKG_MSYS_CYGUTILS_BIN%"
	call :download msys-patch-bin "%PKG_MSYS_PATCH_BIN%"
	call :download msys-diffutils-bin "%PKG_MSYS_DIFFUTILS_BIN%"
	call :download msys-grep-bin "%PKG_MSYS_GREP_BIN%"
	call :download msys-tar-ext "%PKG_MSYS_TAR_EXT%"
	call :download msys-tar-bin "%PKG_MSYS_TAR_BIN%"
	call :download msys-less-bin "%PKG_MSYS_LESS_BIN%"
	call :download msys-gzip-bin "%PKG_MSYS_GZIP_BIN%"
	call :download msys-m4-bin "%PKG_MSYS_M4_BIN%"
	call :download msys-sed-bin "%PKG_MSYS_SED_BIN%"
	call :download msys-bison-bin "%PKG_MSYS_BISON_BIN%"
	call :download msys-flex-bin "%PKG_MSYS_FLEX_BIN%"
	rem call :download msys-perl-bin "%PKG_MSYS_PERL_BIN%"
	call :download msys-rebase-bin "%PKG_MSYS_REBASE_BIN%"
	call :download msys-crypt-dll "%PKG_MSYS_CRYPT_DLL%"
	call :download msys-crypt-bin "%PKG_MSYS_CRYPT_BIN%"
	call :download msys-bzip2-dll "%PKG_MSYS_BZIP2_DLL%"
	call :download msys-bzip2-bin "%PKG_MSYS_BZIP2_BIN%"
	call :download msys-openssl-dll "%PKG_MSYS_OPENSSL_DLL%"
	call :download msys-openssl-bin "%PKG_MSYS_OPENSSL_BIN%"
	call :download msys-gmp-dll "%PKG_MSYS_GMP_DLL%"
	call :download msys-guile-dll "%PKG_MSYS_GUILE_DLL%"
	call :download msys-guile-rtm "%PKG_MSYS_GUILE_RTM%"
	call :download msys-guile-bin "%PKG_MSYS_GUILE_BIN%"
	call :download msys-ltdl-dll "%PKG_MSYS_LTDL_DLL%"
	call :download msys-xml2-dll "%PKG_MSYS_XML2_DLL%"
	call :download msys-xml2-bin "%PKG_MSYS_XML2_BIN%"
	call :download msys-make-bin "%PKG_MSYS_MAKE_BIN%"
	call :download msys-locate-bin "%PKG_MSYS_LOCATE_BIN%"
	call :download msys-findutils-bin "%PKG_MSYS_FINDUTILS_BIN%"
	call :download msys-mktemp-bin "%PKG_MSYS_MKTEMP_BIN%"
	call :download msys-gawk-bin "%PKG_MSYS_GAWK_BIN%"
	call :download msys-libgcc-dll "%PKG_MSYS_LIBGCC_DLL%"
	call :download msys-flip-bin "%PKG_MSYS_FLIP_BIN%"
	
	call :download mingw-basicbsdtar-bin "%PKG_MINGW_BASICBSDTAR_BIN%"
	call :download mingw-autoconf-bin "%PKG_MINGW_AUTOCONF_BIN%"
	call :download mingw-autoconfwrapper-bin "%PKG_MINGW_AUTOCONFWRAPPER_BIN%"
	call :download mingw-automake-1_4-bin "%PKG_MINGW_AUTOMAKE_1_4_BIN%"
	call :download mingw-automake-1_8-bin "%PKG_MINGW_AUTOMAKE_1_8_BIN%"
	call :download mingw-automake-1_10-bin "%PKG_MINGW_AUTOMAKE_1_10_BIN%"
	call :download mingw-automake-bin "%PKG_MINGW_AUTOMAKE_BIN%"
	call :download mingw-automakewrapper-bin "%PKG_MINGW_AUTOMAKEWRAPPER_BIN%"
	call :download mingw-ltdl-bin "%PKG_MINGW_LTDL_DLL%"
	call :download mingw-libtool-bin "%PKG_MINGW_LIBTOOL_BIN%"
	call :download mingw-pexports-bin "%PKG_MINGW_PEXPORTS_BIN%"
	call :download mingw-gettext-dev "%PKG_MINGW_GETTEXT_DEV%"
	call :download mingw-make-bin "%PKG_MINGW_MAKE_BIN%"
	call :download mingw-glib-dll "%PKG_MINGW_GLIB_DLL%"
	call :download mingw-pkg-config-bin "%PKG_MINGW_PKGCONFIG_BIN%"
	call :download mingw-gettextpo-dll "%PKG_MINGW_LIBGETTEXTPO_DLL%"
	call :download mingw-intl-dll "%PKG_MINGW_INTL_DLL%"
	call :download mingw-libasprintf-dll "%PKG_MINGW_LIBASPRINTF_DLL%"
	call :download mingw-gettext-bin "%PKG_MINGW_GETTEXT_BIN%"
	call :download mingw-libcharset-dll "%PKG_MINGW_LIBCHARSET_DLL%"
	call :download mingw-libiconv-dll "%PKG_MINGW_LIBICONV_DLL%"
	call :download mingw-libiconv-bin "%PKG_MINGW_LIBICONV_BIN%"
	REM call :download mingw-gcc-core-bin "%PKG_MINGW_GCC_CORE_BIN%"
	REM call :download mingw-gcc-cpp-bin "%PKG_MINGW_GCC_CPP_BIN%"
	REM call :download mingw-gcc-objc-bin "%PKG_MINGW_GCC_OBJC_BIN%"
	REM call :download mingw-libgnat-dll "%PKG_MINGW_LIBGNAT_DLL%"
	REM call :download mingw-libgomp-dll "%PKG_MINGW_LIBGOMP_DLL%"
	REM call :download mingw-libgcc-dll "%PKG_MINGW_LIBGCC_DLL%"
	REM call :download mingw-libstdcpp-dll "%PKG_MINGW_LIBSTDCPP_DLL%"
	REM call :download mingw-libobjc-dll "%PKG_MINGW_LIBOBJC_DLL%"
	REM call :download mingw-libssp-dll "%PKG_MINGW_LIBSSP_DLL%"
	REM call :download mingw-binutils-bin "%PKG_MINGW_BINUTILS_BIN%"
	REM call :download mingw-w32api-dev "%PKG_MINGW_W32API_DEV%"
	REM call :download mingw-libgmp-dll "%PKG_MINGW_LIBGMP_DLL%"
	REM call :download mingw-libmpfr-dll "%PKG_MINGW_LIBMPFR_DLL%"
	REM call :download mingw-pthreads-dll "%PKG_MINGW_PTHREADS_DLL%"
	REM call :download mingw-libmpc-dll "%PKG_MINGW_LIBMPC_DLL%"
	REM call :download mingw-mingwrt-dll "%PKG_MINGW_MINGWRT_DLL%"
	
	rem Download DXVA2 for use by FFmpeg AVHWAccel API
	wget --no-check-certificate -O dxva2api.h "http://downloads.videolan.org/pub/videolan/testing/contrib/dxva2api.h"
	
	rem Get strawberry perl
	rem wget --no-check-certificate -O perl.zip "%PKG_STRAWBERRY_PERL_BIN%"
	
	rem Get msysGit which has a newer version of perl
	wget --no-check-certificate -O git.7z "%PKG_MSYSGIT_BIN%"
	
	rem Get activestate perl
	wget --no-check-certificate -O perl.zip "%PKG_ACTIVESTATE_PERL_BIN%"
	
	rem Get MinGW-w64 gcc
	REM wget --no-check-certificate -O gcc-x86.zip "%PKG_MINGW_W64_GCC_X86_BIN%"
	REM wget --no-check-certificate -O gcc-x86_64.zip "%PKG_MINGW_W64_GCC_X86_64_BIN%"
	
	rem Get TDM gcc (mingw version)
	REM call :download tdm-gcc-mingw-core-bin "%PKG_TDM_GCC_MINGW_CORE_BIN%"
	REM call :download tdm-gcc-mingw-binutils-bin "%PKG_TDM_GCC_MINGW_BINUTILS_BIN%"
	REM call :download tdm-gcc-mingw-mingwrt-dev "%PKG_TDM_GCC_MINGW_MINGWRT_DEV%"
	REM call :download tdm-gcc-mingw-mingwrt-dll "%PKG_TDM_GCC_MINGW_MINGWRT_DLL%"
	REM call :download tdm-gcc-mingw-w32api-dev "%PKG_TDM_GCC_MINGW_W32API_DEV%"
	REM call :download tdm-gcc-mingw-cpp-bin "%PKG_TDM_GCC_MINGW_CPP_BIN%"
	REM call :download tdm-gcc-mingw-objc-bin "%PKG_TDM_GCC_MINGW_OBJC_BIN%"
	
	rem Get TDM gcc (mingw-w64 version)
	call :download tdm-gcc-mingw64-core-bin "%PKG_TDM_GCC_MINGW64_CORE_BIN%"
	call :download tdm-gcc-mingw64-binutils-bin "%PKG_TDM_GCC_MINGW64_BINUTILS_BIN%"
	call :download tdm-gcc-mingw64-mingwrt-runtime "%PKG_TDM_GCC_MINGW64_MINGWRT_RUNTIME%"
	call :download tdm-gcc-mingw64-cpp-bin "%PKG_TDM_GCC_MINGW64_CPP_BIN%"
	call :download tdm-gcc-mingw64-objc-bin "%PKG_TDM_GCC_MINGW64_OBJC_BIN%"
	
	cd /d "%MSYSDIR%"
)

if "%UNTAR%" == "1" (
	cd /d "%TMPDIR%"
	
	rem Don't use strawberry perl b/c msys git has a newer 
	rem version of perl available plus it gives us git commands.
	rem cd /d "%TMPDIR%"
	rem 7z -y "-operl\" x perl.zip
	rem cd /d "perl\"
	rem mkdir "%DEST_MSYS_DIR%\perl"
	rem mkdir "%DEST_MSYS_DIR%\perl\bin"
	rem copy relocation.pl "%DEST_MSYS_DIR%"
	rem xcopy /Y /K /H /E "c\bin\*.dll" "%DEST_MSYS_DIR%\perl\bin\"
	rem xcopy /Y /K /H /E "perl\*" "%DEST_MSYS_DIR%\perl\"
	rem cd ..
	rem cd /d "%DEST_MSYS_DIR%"
	rem perl\bin\perl.exe relocation.pl
	rem del relocation.pl
	rem "%TOOLSDIR%\rm" -rf "%TMPDIR%\perl\"
	rem cd /d "%TMPDIR%"
	
	rem Get git commands + newer version of perl
	7za -y "-o%MSYSDIR%" x git.7z
	
	rem Install activestate perl
	7za -y "-o%TMPDIR%" x perl.zip
	cd /d "%PKG_ACTIVESTATE_PERL_DIR%"
	call Installer.bat
	cd /d "%TMPDIR%"
	
	rem This file isn't in the typical msys/mingw structure, so we have to 
	rem treat it very differently.
	REM move mingw-gcc-bin.tar.lzma mingw-gcc-bin.7z
	REM 7z -y x mingw-gcc-bin.7z
	REM cd "%GCCDIR%"
	REM xcopy /Y /K /H /E ".\*" "%MINGWDIR%"
	REM cd ..
	
	call :extract msys-core-bin %MSYSDIR%
	call :extract msys-core-ext %MSYSDIR%
	call :extract msys-rxvt-bin %MSYSDIR%
	call :extract msys-bash-bin %MSYSDIR%
	call :extract msys-regex-dll %MSYSDIR%
	call :extract msys-termcap-dll %MSYSDIR%
	call :extract msys-core-utils-ext %MSYSDIR%
	call :extract msys-core-utils-bin %MSYSDIR%
	call :extract msys-iconv-dll %MSYSDIR%
	call :extract msys-iconv-bin %MSYSDIR%
	call :extract msys-charset-dll %MSYSDIR%
	call :extract msys-zlib-dll %MSYSDIR%
	call :extract msys-intl-dll %MSYSDIR%
	call :extract msys-gettext-dll %MSYSDIR%
	call :extract msys-asprintf-dll %MSYSDIR%
	call :extract msys-gettext-bin %MSYSDIR%
	call :extract msys-wget-bin %MSYSDIR%
	call :extract msys-unzip-bin %MSYSDIR%
	call :extract msys-zip-bin %MSYSDIR%
	call :extract msys-opts-dll %MSYSDIR%
	call :extract msys-autogen-bin %MSYSDIR%
	call :extract msys-popt-bin %MSYSDIR%
	call :extract msys-cygutils-dos2unix-bin %MSYSDIR%
	call :extract msys-cygutils-bin %MSYSDIR%
	call :extract msys-patch-bin %MSYSDIR%
	call :extract msys-diffutils-bin %MSYSDIR%
	call :extract msys-grep-bin %MSYSDIR%
	call :extract msys-tar-ext %MSYSDIR%
	call :extract msys-tar-bin %MSYSDIR%
	call :extract msys-less-bin %MSYSDIR%
	call :extract msys-gzip-bin %MSYSDIR%
	call :extract msys-m4-bin %MSYSDIR%
	call :extract msys-sed-bin %MSYSDIR%
	call :extract msys-bison-bin %MSYSDIR%
	call :extract msys-flex-bin %MSYSDIR%
	rem call :extract msys-perl-bin %MSYSDIR%
	call :extract msys-rebase-bin %MSYSDIR%
	call :extract msys-crypt-dll %MSYSDIR%
	call :extract msys-crypt-bin %MSYSDIR%
	call :extract msys-bzip2-dll %MSYSDIR%
	call :extract msys-bzip2-bin %MSYSDIR%
	call :extract msys-openssl-dll %MSYSDIR%
	call :extract msys-openssl-bin %MSYSDIR%
	call :extract msys-gmp-dll %MSYSDIR%
	call :extract msys-guile-dll %MSYSDIR%
	call :extract msys-guile-rtm %MSYSDIR%
	call :extract msys-guile-bin %MSYSDIR%
	call :extract msys-ltdl-dll %MSYSDIR%
	call :extract msys-xml2-dll %MSYSDIR%
	call :extract msys-xml2-bin %MSYSDIR%
	call :extract msys-make-bin %MSYSDIR%
	call :extract msys-locate-bin %MSYSDIR%
	call :extract msys-findutils-bin %MSYSDIR%
	call :extract msys-mktemp-bin %MSYSDIR%
	call :extract msys-gawk-bin %MSYSDIR%
	call :extract msys-libgcc-dll %MSYSDIR%
	
	rem It's a straight up executable from a website
	move msys-flip-bin.tar.lzma "%MSYSDIR%\bin\flip.exe"
	
	call :extract mingw-basicbsdtar-bin %MINGWDIR%
	call :extract mingw-autoconf-bin %MINGWDIR%
	call :extract mingw-autoconfwrapper-bin %MINGWDIR%
	call :extract mingw-automake-1_4-bin %MINGWDIR%
	call :extract mingw-automake-1_8-bin %MINGWDIR%
	call :extract mingw-automake-1_10-bin %MINGWDIR%
	call :extract mingw-automake-bin %MINGWDIR%
	call :extract mingw-automakewrapper-bin %MINGWDIR%
	call :extract mingw-ltdl-bin %MINGWDIR%
	call :extract mingw-libtool-bin %MINGWDIR%
	call :extract mingw-pexports-bin %MINGWDIR%
	call :extract mingw-gettext-dev %MINGWDIR%
	call :extract mingw-gettextpo-dll %MINGWDIR%
	call :extract mingw-intl-dll %MINGWDIR%
	call :extract mingw-libasprintf-dll %MINGWDIR%
	call :extract mingw-gettext-bin %MINGWDIR%
	call :extract mingw-libcharset-dll %MINGWDIR%
	call :extract mingw-libiconv-dll %MINGWDIR%
	call :extract mingw-libiconv-bin %MINGWDIR%
	call :extract mingw-make-bin %MINGWDIR%
	REM call :extract mingw-gcc-core-bin %MINGWDIR%
	REM call :extract mingw-gcc-cpp-bin %MINGWDIR%
	REM call :extract mingw-gcc-objc-bin %MINGWDIR%
	REM call :extract mingw-libgnat-dll %MINGWDIR%
	REM call :extract mingw-libgomp-dll %MINGWDIR%
	REM call :extract mingw-libgcc-dll %MINGWDIR%
	REM call :extract mingw-libstdcpp-dll %MINGWDIR%
	REM call :extract mingw-libobjc-dll %MINGWDIR%
	REM call :extract mingw-libssp-dll %MINGWDIR%
	REM call :extract mingw-binutils-bin %MINGWDIR%
	REM call :extract mingw-w32api-dev %MINGWDIR%
	REM call :extract mingw-libgmp-dll %MINGWDIR%
	REM call :extract mingw-libmpfr-dll %MINGWDIR%
	REM call :extract mingw-pthreads-dll %MINGWDIR%
	REM call :extract mingw-libmpc-dll %MINGWDIR%
	REM call :extract mingw-mingwrt-dll %MINGWDIR%
	
	rem Copy these headers so FFmpeg can pick up hardware-accelerated decoding through DirectX
	rem Not ready yet in mingw-w64, works for mingw32
	REM copy "%TMPDIR%\dxva2api.h" "%MINGWDIR%\i686-pc-mingw32\include\dxva2api.h"
	REM copy "%TMPDIR%\dxva2api.h" "%MINGWDIR%\x86_64-pc-mingw32\include\dxva2api.h"
	REM copy "%TMPDIR%\dxva2api.h" "%MINGWDIR%\x86_64-w64-mingw32\include\dxva2api.h"
	
	rem .zip requires custom handling
	move mingw-glib-dll.tar.lzma mingw-glib-dll.zip
	7za -y "-o%MINGWDIR%" x mingw-glib-dll.zip
	
	move mingw-pkg-config-bin.tar.lzma mingw-pkg-config-bin.zip
	7za -y "-o%MINGWDIR%" x mingw-pkg-config-bin.zip
	
	rem TDM gcc (mingw version)
	REM call :extract tdm-gcc-mingw-core-bin %MINGWDIR%
	REM call :extract tdm-gcc-mingw-binutils-bin %MINGWDIR%
	REM call :extract tdm-gcc-mingw-mingwrt-dev %MINGWDIR%
	REM call :extract tdm-gcc-mingw-mingwrt-dll %MINGWDIR%
	REM call :extract tdm-gcc-mingw-w32api-dev %MINGWDIR%
	REM call :extract tdm-gcc-mingw-cpp-bin %MINGWDIR%
	REM call :extract tdm-gcc-mingw-objc-bin %MINGWDIR%
	
	rem TDM gcc (mingw-w64 version)
	call :extract tdm-gcc-mingw64-core-bin %MINGWDIR%
	call :extract tdm-gcc-mingw64-binutils-bin %MINGWDIR%
	call :extract tdm-gcc-mingw64-mingwrt-runtime %MINGWDIR%
	call :extract tdm-gcc-mingw64-cpp-bin %MINGWDIR%
	call :extract tdm-gcc-mingw64-objc-bin %MINGWDIR%
	
	REM 7za -y "-o%MINGWDIR%" x gcc-x86.zip
	REM 7za -y "-o%MINGWDIR%" x gcc-x86_64.zip
	
	REM cd /d "%MINGWDIR%\bin\"
	REM copy addr2line.exe i686-pc-mingw32-addr2line.exe
	REM copy ar.exe i686-pc-mingw32-ar.exe
	REM copy as.exe i686-pc-mingw32-as.exe
	REM copy "c++filt.exe" "i686-pc-mingw32-c++filt.exe"
	REM copy cpp.exe i686-pc-mingw32-cpp.exe
	REM copy dlltool.exe i686-pc-mingw32-dlltool.exe
	REM copy dllwrap.exe i686-pc-mingw32-dllwrap.exe
	REM copy elfedit.exe i686-pc-mingw32-elfedit.exe
	REM copy gccbug i686-pc-mingw32-gccbug
	REM copy gcov.exe i686-pc-mingw32-gcov.exe
	REM copy gprof.exe i686-pc-mingw32-gprof.exe
	REM copy ld.exe i686-pc-mingw32-ld.exe
	REM copy nm.exe i686-pc-mingw32-nm.exe
	REM copy objcopy.exe i686-pc-mingw32-objcopy.exe
	REM copy objdump.exe i686-pc-mingw32-objdump.exe
	REM copy ranlib.exe i686-pc-mingw32-ranlib.exe
	REM copy readelf.exe i686-pc-mingw32-readelf.exe
	REM copy size.exe i686-pc-mingw32-size.exe
	REM copy strings.exe i686-pc-mingw32-strings.exe
	REM copy strip.exe i686-pc-mingw32-strip.exe
	REM copy windmc.exe i686-pc-mingw32-windmc.exe
	REM copy windres.exe i686-pc-mingw32-windres.exe
	
	cd /d "%MSYSDIR%"
)

if "%CLEAN%" == "1" (
	cd /d "%TMPDIR%"
	
	REM del mingw-gcc-bin.7z
	
	call :clean msys-core-bin
	call :clean msys-core-ext
	call :clean msys-rxvt-bin
	call :clean msys-bash-bin
	call :clean msys-regex-dll
	call :clean msys-termcap-dll
	call :clean msys-core-utils-ext
	call :clean msys-core-utils-bin
	call :clean msys-iconv-dll
	call :clean msys-iconv-bin
	call :clean msys-charset-dll
	call :clean msys-zlib-dll
	call :clean msys-intl-dll
	call :clean msys-gettext-dll
	call :clean msys-asprintf-dll
	call :clean msys-gettext-bin
	call :clean msys-wget-bin
	call :clean msys-unzip-bin
	call :clean msys-zip-bin
	call :clean msys-opts-dll
	call :clean msys-autogen-bin
	call :clean msys-popt-bin
	call :clean msys-cygutils-dos2unix-bin
	call :clean msys-cygutils-bin
	call :clean msys-patch-bin
	call :clean msys-diffutils-bin
	call :clean msys-grep-bin
	call :clean msys-tar-ext
	call :clean msys-tar-bin
	call :clean msys-less-bin
	call :clean msys-gzip-bin
	call :clean msys-m4-bin
	call :clean msys-sed-bin
	call :clean msys-bison-bin
	call :clean msys-flex-bin
	rem call :clean msys-perl-bin
	call :clean msys-rebase-bin
	call :clean msys-crypt-dll
	call :clean msys-crypt-bin
	call :clean msys-bzip2-dll
	call :clean msys-bzip2-bin
	call :clean msys-openssl-dll
	call :clean msys-openssl-bin
	call :clean msys-gmp-dll
	call :clean msys-guile-dll
	call :clean msys-guile-rtm
	call :clean msys-guile-bin
	call :clean msys-ltdl-dll
	call :clean msys-xml2-dll
	call :clean msys-xml2-bin
	call :clean msys-make-bin
	call :clean msys-locate-bin
	call :clean msys-findutils-bin
	call :clean msys-mktemp-bin
	call :clean msys-gawk-bin
	call :clean msys-libgcc-dll
	
	call :clean mingw-basicbsdtar-bin
	call :clean mingw-autoconf-bin
	call :clean mingw-autoconfwrapper-bin
	call :clean mingw-automake-1_4-bin
	call :clean mingw-automake-1_8-bin
	call :clean mingw-automake-1_10-bin
	call :clean mingw-automake-bin
	call :clean mingw-automakewrapper-bin
	call :clean mingw-ltdl-bin
	call :clean mingw-libtool-bin
	call :clean mingw-pexports-bin
	call :clean mingw-gettext-dev
	call :clean mingw-gettextpo-dll
	call :clean mingw-intl-dll
	call :clean mingw-libasprintf-dll
	call :clean mingw-gettext-bin
	call :clean mingw-libcharset-dll
	call :clean mingw-libiconv-dll
	call :clean mingw-libiconv-bin
	call :clean mingw-make-bin
	REM call :clean mingw-gcc-core-bin
	REM call :clean mingw-gcc-cpp-bin
	REM call :clean mingw-gcc-objc-bin
	REM call :clean mingw-libgnat-dll
	REM call :clean mingw-libgomp-dll
	REM call :clean mingw-libgcc-dll
	REM call :clean mingw-libstdcpp-dll
	REM call :clean mingw-libobjc-dll
	REM call :clean mingw-libssp-dll
	REM call :clean mingw-binutils-bin
	REM call :clean mingw-w32api-dev
	REM call :clean mingw-libgmp-dll
	REM call :clean mingw-libmpfr-dll
	REM call :clean mingw-pthreads-dll
	REM call :clean mingw-libmpc-dll
	REM call :clean mingw-mingwrt-dll
	
	rem TDM gcc (mingw version)
	REM call :clean tdm-gcc-mingw-core-bin
	REM call :clean tdm-gcc-mingw-binutils-bin
	REM call :clean tdm-gcc-mingw-mingwrt-dev
	REM call :clean tdm-gcc-mingw-mingwrt-dll
	REM call :clean tdm-gcc-mingw-w32api-dev
	REM call :clean tdm-gcc-mingw-cpp-bin
	REM call :clean tdm-gcc-mingw-objc-bin
	
	rem TDM gcc (mingw-w64 version)
	call :clean tdm-gcc-mingw64-core-bin
	call :clean tdm-gcc-mingw64-binutils-bin
	call :clean tdm-gcc-mingw64-mingwrt-runtime
	call :clean tdm-gcc-mingw64-cpp-bin
	call :clean tdm-gcc-mingw64-objc-bin
	
	REM del gcc-x86.zip
	REM del gcc-x86_64.zip
	
	del git.7z
	
	rem del perl.zip
	
	del dxva2api.h
	
	del mingw-glib-dll.zip
	del mingw-pkg-config-bin.zip
	
	rem Clean gettext libs and includes that we don't want hanging around
	"%TOOLSDIR%\rm" -rf "%MSYSDIR%\lib\lib*"
	"%TOOLSDIR%\rm" -rf "%MSYSDIR%\include\*"
	
	rem Remove temp directory
	cd /d "%TOPDIR%"
	"%TOOLSDIR%\rm" -rf "%TMPDIR%"
	
	cd /d "%MSYSDIR%"
)

rem Move to known directory
cd /d "%MSYSDIR%"

rem Apply patches
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-pc-mingw32\include\ws2tcpip.h" "%MINGWDIR%\i686-pc-mingw32\include\ws2tcpip.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\include\ws2tcpip.h" "%MINGWDIR%\include\ws2tcpip.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-w64-mingw32\include\wincrypt.h" "%MINGWDIR%\i686-w64-mingw32\include\wincrypt.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-w64-mingw32\include\ws2tcpip.h" "%MINGWDIR%\i686-w64-mingw32\include\ws2tcpip.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-w64-mingw32\include\GL\gl.h" "%MINGWDIR%\i686-w64-mingw32\include\GL\gl.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-w64-mingw32\include\GL\glu.h" "%MINGWDIR%\i686-w64-mingw32\include\GL\glu.h"
REM copy /Y "%PATCHESDIR%\msys\mingw\i686-w64-mingw32\include\GL\glext.h" "%MINGWDIR%\i686-w64-mingw32\include\GL\glext.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\_mingw_mac.h" "%MINGWDIR%\x86_64-w64-mingw32\include\_mingw_mac.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\iphlpapi.h" "%MINGWDIR%\x86_64-w64-mingw32\include\iphlpapi.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\wincrypt.h" "%MINGWDIR%\x86_64-w64-mingw32\include\wincrypt.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\ws2tcpip.h" "%MINGWDIR%\x86_64-w64-mingw32\include\ws2tcpip.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\winnt.h" "%MINGWDIR%\x86_64-w64-mingw32\include\winnt.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\d3d9.h" "%MINGWDIR%\x86_64-w64-mingw32\include\d3d9.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\d3d9caps.h" "%MINGWDIR%\x86_64-w64-mingw32\include\d3d9caps.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\d3d9types.h" "%MINGWDIR%\x86_64-w64-mingw32\include\d3d9types.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\GL\gl.h" "%MINGWDIR%\x86_64-w64-mingw32\include\GL\gl.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\GL\glu.h" "%MINGWDIR%\x86_64-w64-mingw32\include\GL\glu.h"
copy /Y "%PATCHESDIR%\msys\mingw\x86_64-w64-mingw32\include\GL\glext.h" "%MINGWDIR%\x86_64-w64-mingw32\include\GL\glext.h"

rem Make a shortcut to our shell
"%TOOLSDIR%\mklink" "%TOPDIR%\msys.bat.lnk" "/q" "%DEST_MSYS_DIR%\msys.bat"
goto done



:download %filename% %url%
setlocal
set filename=%~1&set url=%~2
set currdirectory=%CD%
cd /d "%TMPDIR%"
wget --no-check-certificate -O %filename%.tar.lzma "%url%"
cd /d "%currdirectory%"
endlocal&goto :EOF

:extract %filename% %destdir%
setlocal ENABLEEXTENSIONS
set filename=%~1&set destdir=%~2
set currdirectory=%CD%
cd /d "%TMPDIR%"
7za -y x %filename%.tar.lzma
7za -y "-o%destdir%" x %filename%.tar
del %filename%.tar
rem Don't remove the .tar.lzma file -- leave it for the cleaning stage
cd /d "%currdirectory%"
endlocal&goto :EOF

:clean %filename%
setlocal ENABLEEXTENSIONS
set filename=%~1
set currdirectory=%CD%
cd /d "%TMPDIR%"
del %filename%.tar.lzma
cd /d "%currdirectory%"
endlocal&goto :EOF

:done
cd /d "%CURRDIR%"
rem exit 0
