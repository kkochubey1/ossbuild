#!/bin/sh

###############################################################################
#                                                                             #
#                        Windows x86 (Win32) Build                            #
#                                                                             #
# Builds all dependencies for this platform.                                  #
#                                                                             #
# Useful utilities:                                                           #
#    1) Finding libs that are not branded correctly                           #
#           find *.lib *.dll.a  -type f -print0 | xargs -0 grep -L ossbuild   #
#                                                                             #
###############################################################################

TOP=$(dirname $0)/..

#Global directories
PERL_BIN_DIR=/C/Perl/bin
if [ ! -d "$PERL_BIN_DIR" ]; then 
	PERL_BIN_DIR=/C/Perl64/bin
fi

#Global flags
CFLAGS="$CFLAGS -m32 -mms-bitfields -pipe -DWINVER=0x0501 "
CPPFLAGS="$CPPFLAGS -DMINGW64 -D__MINGW64__ -DMINGW32 -D__MINGW32__"
LDFLAGS="-Wl,--enable-auto-image-base -Wl,--enable-auto-import -Wl,--enable-runtime-pseudo-reloc-v2 -Wl,--kill-at "
CXXFLAGS="${CFLAGS}"
MAKE_PARALLEL_FLAGS="-j2"

#Call common startup routines to load a bunch of variables we'll need
. $TOP/Shared/Scripts/Common.sh
common_startup "Windows" "Win32" "Release" "x86" "" ""

#Select which MS CRT we want to build against (e.g. msvcr90.dll)
. $ROOT/Shared/Scripts/CRT-x86.sh
crt_startup

#Setup library versions
. $ROOT/Shared/Scripts/Version.sh

save_prefix ${DefaultPrefix}

#Move to intermediate directory
cd "$IntDir"

#Add MS build tools
setup_ms_build_env_path

#Ensure that whoami and hostname provide helpful values
create_whoami_windows
create_hostname_windows

#clear_flags

#No prefix from here out
clear_prefix


#Not using dwarf2 yet
###gcc_dw2
##if [ ! -f "$BinDir/libgcc_s_dw2-1.dll" ]; then
##	#Needed for new dwarf2 exception handling
##	copy_files_to_dir "$LIBRARIES_PATCH_DIR/gcc_dw2/libgcc_s_dw2-1.dll" "$BinDir"
##fi

#gnome-common
if [ ! -f "$BinDir/gnome-autogen.sh" ]; then 
	unpack_bzip2_and_move "gnome-common.tar.bz2" "$PKG_DIR_GNOME_COMMON"
	mkdir_and_move "$IntDir/gnome-common"
	
	$PKG_DIR/configure --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make && make install
fi

#proxy-libintl
if [ ! -f "$LibDir/intl.lib" ]; then 
	unpack_zip_and_move_windows "proxy-libintl.zip" "proxy-libintl" "proxy-libintl"
	mkdir_and_move "$IntDir/proxy-libintl"
	copy_files_to_dir "$PKG_DIR/*" .
	$gcc -Wall -I"$IncludeDir" -c libintl.c
	$ar rc "$LibDir/libintl.a" libintl.o
	cp "$LibDir/libintl.a" "$LibDir/intl.lib"
	$strip --strip-unneeded "$LibDir/intl.lib"
	cp libintl.h "$IncludeDir"
	generate_libtool_la_windows "libintl.la" "" "libintl.a"
fi


#Update flags to make sure we don't try and export intl (gettext) functions more than once
#Setting it to ORIG_LDFLAGS ensures that any calls to reset_flags will include these changes.
ORIG_LDFLAGS="$ORIG_LDFLAGS -Wl,--exclude-libs=libintl.a -Wl,--add-stdcall-alias"
reset_flags


#fribidi
if [ ! -f "$BinDir/libfribidi-0.dll" ]; then
	unpack_gzip_and_move "fribidi.tar.gz" "$PKG_DIR_FRIBIDI"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/fribidi/fribidi.patch"
	mkdir_and_move "$IntDir/fribidi"
	
	cd "$PKG_DIR/"
	./bootstrap
	
	cd "$IntDir/fribidi"
	$PKG_DIR/configure --with-glib=no --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_key "lib/" "Makefile" "am__append_1" ""
	make && make install

	sed -e '/LIBRARY libfribidi/d' -e 's/DATA//g' .libs/libfribidi-0.dll.def > in-mod.def
	$MSLIB /name:libfribidi-0.dll /out:fribidi.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	reset_flags
fi

#liboil
if [ ! -f "$BinDir/lib${Prefix}oil-0.3-0.dll" ]; then
	unpack_gzip_and_move "liboil.tar.gz" "$PKG_DIR_LIBOIL"
	mkdir_and_move "$IntDir/liboil"
	
	CFLAGS="$CFLAGS -DHAVE_SYMBOL_UNDERSCORE=1"
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make && make install
	
	#Do it this way b/c changing libname_spec causes it to not build correctly
	mv "$BinDir/liboil-0.3-0.dll" "$BinDir/lib${Prefix}oil-0.3-0.dll"
	pexports "$BinDir/lib${Prefix}oil-0.3-0.dll" > "in.def"
	sed -e '/^LIBRARY/d' -e 's/DATA//g' in.def > in-mod.def
	$dlltool --dllname lib${Prefix}oil-0.3-0.dll -d "in-mod.def" -l lib${Prefix}oil-0.3.dll.a
	cp -p "lib${Prefix}oil-0.3.dll.a" "$LibDir/liboil-0.3.dll.a"
	$MSLIB /name:lib${Prefix}oil-0.3-0.dll /out:oil-0.3.lib /machine:$MSLibMachine /def:liboil/.libs/liboil-0.3-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	change_key "$LibDir/" "liboil-0.3.la" "dlname" "\'\.\.\/bin\/lib${Prefix}oil-0\.3-0\.dll\'"
	
	#change_libname_spec
	#make && make install
	#update_library_names_windows "lib${Prefix}oil-0.3.dll.a" "liboil-0.3.la"
	#
	#$MSLIB /name:lib${Prefix}oil-0.3-0.dll /out:oil-0.3.lib /machine:$MSLibMachine /def:liboil/.libs/lib${Prefix}oil-0.3-0.dll.def
	#move_files_to_dir "*.exp *.lib" "$LibDir"
	
	reset_flags
fi

#pthreads
PthreadsPrefix=lib${Prefix}
if [ "${Prefix}" = "" ]; then
	PthreadsPrefix=""
fi
if [ ! -f "$BinDir/${PthreadsPrefix}pthreadGC2.dll" ]; then 
	unpack_gzip_and_move "pthreads-w32.tar.gz" "$PKG_DIR_PTHREADS"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/pthreads-w32/sched.h.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/pthreads-w32/pthreads-detach.patch"
	mkdir_and_move "$IntDir/pthreads"

	cd "$PKG_DIR"
	change_package "${PthreadsPrefix}pthreadGC\$(DLL_VER).dll" "." "GNUmakefile" "GC_DLL"
	make "CC=${gcc}" "RC=${windres}" "CROSS=${BuildTripletDash}" "PTW32_FLAGS=-D_TIMESPEC_DEFINED" ${MAKE_PARALLEL_FLAGS} GC-inlined
	$MSLIB /name:${PthreadsPrefix}pthreadGC2.dll /out:pthreadGC2.lib /machine:$MSLibMachine /def:pthread.def
	cp -p pthreadGC2.lib libpthreadGC2.dll.a
	copy_files_to_dir "*.exp *.lib *.a" "$LibDir"
	copy_files_to_dir "*.dll" "$BinDir"
	copy_files_to_dir "pthread.h sched.h" "$IncludeDir"
	
	mv "libpthreadGC2.dll.a" "$LibDir/"
	cp -p "$LibDir/libpthreadGC2.dll.a" "$LibDir/libpthread.dll.a"
	cp -p "$LibDir/libpthreadGC2.dll.a" "$LibDir/libpthreads.dll.a"
	rm -f "$LibDir/libpthreadGC2.a"
	
	generate_libtool_la_windows "libpthreadGC2.la" "${PthreadsPrefix}pthreadGC2.dll" "libpthreadGC2.dll.a"
fi

#orc (oil runtime compiler)
if [ ! -f "$BinDir/lib${Prefix}orc-0.4-0.dll" ]; then
	unpack_gzip_and_move "orc.tar.gz" "$PKG_DIR_ORC"
	mkdir "$IntDir/orc"

	cd "$IntDir/orc"
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir LDFLAGS="$LDFLAGS -lpthread" CPPFLAGS="-D_TIMESPEC_DEFINED"

	make ${MAKE_PARALLEL_FLAGS} && make install
	
	$MSLIB /name:lib${Prefix}orc-0.4-0.dll /out:orc-0.4.lib /machine:$MSLibMachine /def:orc/.libs/lib${Prefix}orc-0.4-0.dll.def
	$MSLIB /name:lib${Prefix}orc-test-0.4-0.dll /out:orc-test-0.4.lib /machine:$MSLibMachine /def:orc-test/.libs/lib${Prefix}orc-test-0.4-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
fi

#libiconv
if [ ! -f "$BinDir/libiconv-2.dll" ]; then
	unpack_gzip_and_move "libiconv.tar.gz" "$PKG_DIR_LIBICONV"
	mkdir_and_move "$IntDir/libiconv"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install

	pexports "$BinDir/libiconv-2.dll" > libiconv.def
	sed -e '/LIBRARY libiconv/d' -e 's/DATA//g' libiconv.def > libiconv-mod.def
	$MSLIB /name:libiconv-2.dll /out:iconv.lib /machine:$MSLibMachine /def:libiconv-mod.def
	
	pexports "$BinDir/libcharset-1.dll" > libcharset.def
	sed -e '/LIBRARY libcharset/d' -e 's/DATA//g' libcharset.def > libcharset-mod.def
	$MSLIB /name:libcharset-1.dll /out:charset.lib /machine:$MSLibMachine /def:libcharset-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
fi

# IconvPrefix=lib${Prefix}
# if [ "${Prefix}" = "" ]; then
	# IconvPrefix=""
# fi
#win-iconv
# if [ ! -f "$BinDir/${IconvPrefix}iconv.dll" ]; then 
	# unpack_bzip2_and_move "win-iconv.tar.bz2" "$PKG_DIR_WIN_ICONV"
	
	# make iconv.dll
	# make libiconv.a
	# mv "iconv.dll" "${IconvPrefix}iconv.dll"
	
	# $MSLIB /name:${IconvPrefix}iconv.dll /out:iconv.lib /machine:$MSLibMachine /def:iconv.def
	# move_files_to_dir "*.dll" "$BinDir"
	# move_files_to_dir "*.exp *.lib" "$LibDir"
	# move_files_to_dir "*.h" "$IncludeDir"
	# copy_files_to_dir "*.a" "$LibDir"
	
	# generate_libtool_la_windows "libiconv.la" "" "libiconv.a"
	# make clean
# fi

#zlib
ZlibPrefix=lib${Prefix}
if [ "${Prefix}" = "" ]; then
	ZlibPrefix=""
fi
#Can't use separate build dir
if [ ! -f "$BinDir/${ZlibPrefix}z.dll" ]; then 
	unpack_bzip2_and_move "zlib.tar.bz2" "$PKG_DIR_ZLIB"
	mkdir_and_move "$IntDir/zlib"
	cd "$PKG_DIR"
	
	change_package "${ZlibPrefix}z.dll" "win32" "Makefile.gcc" "SHAREDLIB"

	cp -p contrib/asm686/match.S ./match.S
	make "CC=${gcc}" "RC=${windres}" "CROSS=${BuildTripletDash}" LOC=-DASMV OBJA=match.o -fwin32/Makefile.gcc
	#make -fwin32/Makefile.gcc ${ZlibPrefix}z.dll
	INCLUDE_PATH=$IncludeDir LIBRARY_PATH=$BinDir make install -fwin32/Makefile.gcc
	
	cp -p ${ZlibPrefix}z.dll "$BinDir"
	cp -p "libzdll.a" "$LibDir/libz.dll.a"
	rm -f "$BinDir/libz.a"
	
	$MSLIB /name:${ZlibPrefix}z.dll /out:z.lib /machine:$MSLibMachine /def:win32/zlib.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	generate_libtool_la_windows "libz.la" "${ZlibPrefix}z.dll" "libz.dll.a"
fi

#bzip2
if [ ! -f "$BinDir/lib${Prefix}bz2.dll" ]; then 
	unpack_gzip_and_move "bzip2.tar.gz" "$PKG_DIR_BZIP2"
	mkdir_and_move "$IntDir/bzip2"
	
	cd "$PKG_DIR"
	
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c blocksort.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c huffman.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c crctable.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c randtable.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c compress.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c decompress.c
	$gcc -DDLL_EXPORT -Wall -Winline -O2 -D_FILE_OFFSET_BITS=64 -c bzlib.c
	rm -f libbz2.a
	$ar cq libbz2.a blocksort.o huffman.o crctable.o randtable.o compress.o decompress.o bzlib.o
	$gcc -Wl,--out-implib=libbz2.dll.a -Wl,--export-all-symbols -Wl,--enable-auto-import -shared -o lib${Prefix}bz2.dll blocksort.o huffman.o crctable.o randtable.o compress.o decompress.o bzlib.o
	
	cp -p libbz2.a "$LibDir/libbz2.a"
	cp -p lib${Prefix}bz2.dll "$BinDir/"
	
	pexports lib${Prefix}bz2.dll > "in.def"
	sed -e 's/DATA//g' in.def > in-mod.def
	cp -p libbz2.dll.a "$LibDir/"
	
	make "CC=$gcc" "AR=$ar" "RANLIB=$ranlib" "CFLAGS=$CFLAGS -O2 -D_FILE_OFFSET_BITS=64"
	make install PREFIX=$InstallDir
	remove_files_from_dir "*.exe"
	remove_files_from_dir "*.so.*"
	
	$MSLIB /name:lib${Prefix}bz2.dll /out:bz2.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	copy_files_to_dir "bzlib.h" "$IncludeDir"
	
	generate_libtool_la_windows "libbz2.la" "lib${Prefix}bz2.dll" "libbz2.dll.a"
	
	reset_flags
fi

#glew
GlewPrefix=lib${Prefix}
if [ "${Prefix}" = "" ]; then
	GlewPrefix=""
fi
if [ ! -f "$BinDir/${GlewPrefix}glew32.dll" ]; then
	unpack_gzip_and_move "glew.tar.gz" "$PKG_DIR_GLEW"
	mkdir_and_move "$IntDir/glew"

	cd "$PKG_DIR"
	change_package "${GlewPrefix}\$(NAME).dll" "config" "Makefile.mingw" "LIB.SONAME"
	change_package "${GlewPrefix}\$(NAME).dll" "config" "Makefile.mingw" "LIB.SHARED"
	make "CC=$gcc" "LD=$gcc" "AR=$ar" ${MAKE_PARALLEL_FLAGS}
	
	cd "lib"
	strip "libglew32.dll.a"
	
	pexports "${GlewPrefix}glew32.dll" > in.def
	sed -e '/LIBRARY glew32/d' -e 's/DATA//g' in.def > in-mod.def
	$MSLIB /name:${GlewPrefix}glew32.dll /out:glew32.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	cp -f "${GlewPrefix}glew32.dll" "$BinDir"
	cp -f "libglew32.dll.a" "$LibDir"
	
	cd "../include/GL/"
	mkdir -p "$IncludeDir/GL/"
	copy_files_to_dir "glew.h wglew.h" "$IncludeDir/GL/"
	
	generate_libtool_la_windows "libglew32.la" "${GlewPrefix}glew32.dll" "libglew32.dll.a"
fi

#expat
if [ ! -f "$BinDir/lib${Prefix}expat-1.dll" ]; then
	unpack_gzip_and_move "expat.tar.gz" "$PKG_DIR_EXPAT"
	mkdir_and_move "$IntDir/expat"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	update_library_names_windows "lib${Prefix}expat.dll.a" "libexpat.la"
	
	copy_files_to_dir "$PKG_DIR/lib/libexpat.def" "$IntDir/expat"
	$MSLIB /name:lib${Prefix}expat-1.dll /out:expat.lib /machine:$MSLibMachine /def:libexpat.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
fi

#libxml2
if [ ! -f "$BinDir/lib${Prefix}xml2-2.dll" ]; then 
	unpack_gzip_and_move "libxml2.tar.gz" "$PKG_DIR_LIBXML2"
	mkdir_and_move "$IntDir/libxml2"
	
	#Compiling with iconv causes an odd dependency on "in.dll" which I think is an intermediary for iconv.a
	$PKG_DIR/configure --with-zlib --with-iconv --with-threads=native --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	
	#We want to ignore pthreads and use native threads. Configure does not 
	#respect our desire so we manipulate the config.h file.
	sed -r s/HAVE_PTHREAD_H/HAVE_WIN32_THREADS/g config.h > tmp1.txt
	sed -r s/HAVE_LIBPTHREAD/HAVE_WIN32/g tmp1.txt > tmp.txt
	rm -f tmp1.txt
	mv tmp.txt config.h
	
	#Build forgets to reference testapi.c correctly
	cp $PKG_DIR/testapi.c .
	
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	update_library_names_windows "lib${Prefix}xml2.dll.a" "libxml2.la"
	
	#Preprocess-only the .def.src file
	#The preprocessor generates some odd "# 1" statements so we want to eliminate those
	CFLAGS="$CFLAGS -I$IncludeDir/libxml2 -I$SharedIncludeDir/libxml2"
	$gcc $CFLAGS -x c -E -D _REENTRANT $PKG_DIR/win32/libxml2.def.src > tmp1.txt
	sed '/# /d' tmp1.txt > tmp2.txt
	sed '/LIBRARY libxml2/d' tmp2.txt > libxml2.def
	reset_flags
	
	#Use the output .def file to generate an MS-compatible lib file
	$MSLIB /name:lib${Prefix}xml2-2.dll /out:xml2.lib /machine:$MSLibMachine /def:libxml2.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	$strip "$LibDir\libxml2.dll.a"
	
	#Unfortunate necessity for linking to the dll later. 
	#Thankfully the linker is compatible w/ the ms .lib format
	cp -p "$LibDir\xml2.lib" "$LibDir\libxml2.dll.a"
fi

##docbook
#if [ ! -f "$BinDir/gtk-doc" ]; then 
#	unpack_bzip2_and_move "docbook.tar.bz2" "$PKG_DIR_DOCBOOK"
#	mkdir_and_move "$IntDir/docbook"
#
#	make
#fi
#
##gtk-doc
##needed for gnome-autogen.sh in pango
#if [ ! -f "$BinDir/gtk-doc" ]; then 
#	unpack_bzip2_and_move "gtk-doc.tar.bz2" "$PKG_DIR_GTK_DOC"
#	mkdir_and_move "$IntDir/gtk-doc"
#	
#	$PKG_DIR/configure --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
#fi

#libjpeg
if [ ! -f "$BinDir/lib${Prefix}jpeg-8.dll" ]; then 
	unpack_gzip_and_move "jpegsrc.tar.gz" "$PKG_DIR_LIBJPEG"
	mkdir_and_move "$IntDir/libjpeg"
	
	CFLAGS="$CFLAGS -O2"
	
	#Configure, compile, and install
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS}
	make install
	
	pexports "$BinDir/lib${Prefix}jpeg-8.dll" > in.def
	sed -e '/LIBRARY lib${Prefix}jpeg/d' -e 's/DATA//g' in.def > in-mod.def
	$MSLIB /name:lib${Prefix}jpeg-8.dll /out:jpeg.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	update_library_names_windows "lib${Prefix}jpeg.dll.a" "libjpeg.la"
	
	reset_flags
fi

#openjpeg
if [ ! -f "$BinDir/lib${Prefix}openjpeg-2.dll" ]; then 
	unpack_gzip_and_move "openjpeg.tar.gz" "$PKG_DIR_OPENJPEG"
	mkdir_and_move "$IntDir/openjpeg"
	
	cd "$PKG_DIR"
	cp "$LIBRARIES_PATCH_DIR/openjpeg/Makefile" .
	
	change_package "${Prefix}openjpeg" "." "Makefile" "TARGET"
	make ${MAKE_PARALLEL_FLAGS} install "CC=$gcc" "AR=$ar" LDFLAGS="-lm" PREFIX=$InstallDir
	make clean
	
	cd "$IntDir/openjpeg"
	pexports "$BinDir/lib${Prefix}openjpeg-2.dll" | sed "s/^_//" > in.def
	$MSLIB /name:lib${Prefix}openjpeg-2.dll /out:openjpeg.lib /machine:$MSLibMachine /def:in.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}openjpeg.dll.a"
fi

#libpng
if [ ! -f "$BinDir/lib${Prefix}png14-14.dll" ]; then 
	unpack_gzip_and_move "libpng.tar.gz" "$PKG_DIR_LIBPNG"
	mkdir_and_move "$IntDir/libpng"	
	
	#png functions are not being properly exported
	cd "$PKG_DIR"
	change_key "." "Makefile.am" "libpng@PNGLIB_MAJOR@@PNGLIB_MINOR@_la_LDFLAGS" "-no-undefined\ -export-symbols-regex\ \'\^\(png\|_png\|png_\)\.\*\'\ \\\\"
	automake
	
	cd "$IntDir/libpng"
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	pexports "$BinDir/lib${Prefix}png14-14.dll" > in.def
	sed -e '/LIBRARY lib${Prefix}png14-14.dll/d' -e '/DATA/d' in.def > in-mod.def
	$MSLIB /name:lib${Prefix}png14-14.dll /out:png14.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	update_library_names_windows "lib${Prefix}png14.dll.a" "libpng14.la"
	cp -f -p "$LibDir/libpng14.la" "$LibDir/libpng.la" 
	cp -f -p "$PkgConfigDir/libpng14.pc" "$PkgConfigDir/libpng12.pc"
fi

#libtiff
if [ ! -f "$BinDir/lib${Prefix}tiff-3.dll" ]; then 
	unpack_gzip_and_move "tiff.tar.gz" "$PKG_DIR_LIBTIFF"
	mkdir_and_move "$IntDir/tiff"
	
	#Configure, compile, and install
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cp -p "$PKG_DIR/libtiff/libtiff.def" .
	$MSLIB /name:lib${Prefix}tiff-3.dll /out:tiff.lib /machine:$MSLibMachine /def:libtiff.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	update_library_names_windows "lib${Prefix}tiff.dll.a" "libtiff.la"
	update_library_names_windows "lib${Prefix}tiffxx.dll.a" "libtiffxx.la"
	
	reset_flags
fi

#libvpx
if [ ! -f "$LibDir/libvpx.a" ]; then 
	unpack_bzip2_and_move "libvpx.tar.bz2" "$PKG_DIR_LIBVPX"
	mkdir_and_move "$IntDir/libvpx"
	
	#Configure, compile, and install
	#--enable-shared isn't available for windows yet
	CC="$gcc" LD="$gcc" $PKG_DIR/configure --target=x86-win32-gcc --enable-vp8 --enable-psnr --enable-runtime-cpu-detect --prefix=$InstallDir --libdir=$LibDir
	make ${MAKE_PARALLEL_FLAGS}
	make install
	
	cp -p "$LibDir/libvpx.a" "$LibDir/vpx.lib"
	$strip --strip-unneeded "$LibDir/vpx.lib"
	
	generate_libtool_la_windows "libvpx.la" "" "libvpx.a"
	
	reset_flags
fi

#glib
if [ ! -f "$BinDir/lib${Prefix}glib-2.0-0.dll" ]; then 
	unpack_bzip2_and_move "glib.tar.bz2" "$PKG_DIR_GLIB"
	mkdir_and_move "$IntDir/glib"
	
	#Need to get rid of MS build tools b/c the makefile call is incorrectly passing it msys-style paths.
	reset_path
	
	lt_cv_deplibs_check_method='pass_all' \
	CC="$gcc -mtune=i686 -mthreads" \
	LDFLAGS="$LDFLAGS -Wl,--enable-auto-image-base" \
	CFLAGS="$CFLAGS -O2" \
	$PKG_DIR/configure --enable-shared --enable-silent-rules --disable-gtk-doc --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	
	#This errors out when attempting to run glib-genmarshal for the gobject lib.
	#This is expected and will hopefully be corrected in the future. As a result, 
	#we run make once to create the proper dll, then copy it in place, and run make 
	#again.
	make ${MAKE_PARALLEL_FLAGS}

	#glib-genmarshal needs libglib-2.0-0.dll but can't load it b/c it isn't in the path
	#Setting PATH didn't work
	cp -p "$IntDir/glib/glib/.libs/libglib-2.0-0.dll" "gobject/.libs/"
	
	make ${MAKE_PARALLEL_FLAGS} install
	
	#Add in MS build tools again
	setup_ms_build_env_path
	
	cd "$IntDir/glib/gio/.libs"
	$MSLIB /name:lib${Prefix}gio-2.0-0.dll /out:gio-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}gio-2.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	cd "../../glib/.libs"
	$MSLIB /name:lib${Prefix}glib-2.0-0.dll /out:glib-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}glib-2.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	cd "../../gmodule/.libs"
	$MSLIB /name:lib${Prefix}gmodule-2.0-0.dll /out:gmodule-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}gmodule-2.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	cd "../../gobject/.libs"
	$MSLIB /name:lib${Prefix}gobject-2.0-0.dll /out:gobject-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}gobject-2.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	cd "../../gthread/.libs"
	$MSLIB /name:lib${Prefix}gthread-2.0-0.dll /out:gthread-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}gthread-2.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	cd "$LibDir"
	remove_files_from_dir "g*-2.0.def"
	
	#This is silly - but glib doesn't copy this config file even when it's needed.
	#See bug 592773 for more information: http://bugzilla.gnome.org/show_bug.cgi?id=592773
	cp -f "$LibDir/glib-2.0/include/glibconfig.h" "$IncludeDir/glib-2.0/glibconfig.h"
	
	update_library_names_windows "lib${Prefix}glib-2.0.dll.a" "libglib-2.0.la"
	update_library_names_windows "lib${Prefix}gio-2.0.dll.a" "libgio-2.0.la"
	update_library_names_windows "lib${Prefix}gmodule-2.0.dll.a" "libgmodule-2.0.la"
	update_library_names_windows "lib${Prefix}gobject-2.0.dll.a" "libgobject-2.0.la"
	update_library_names_windows "lib${Prefix}gthread-2.0.dll.a" "libgthread-2.0.la"
	
	reset_flags
fi

#atk
if [ ! -f "$BinDir/lib${Prefix}atk-1.0-0.dll" ]; then 
	unpack_bzip2_and_move "atk.tar.bz2" "$PKG_DIR_ATK"
	mkdir_and_move "$IntDir/atk"
	
	#TODO: Remove this in next upgrade. git has this fixed already.
	echo atk_value_get_minimum_increment >> $PKG_DIR/atk/atk.symbols
	
	#Need to get rid of MS build tools b/c the makefile call can't find the .def and 
	#also b/c it generates .lib files that don't reference a branded dll
	reset_path
	
	#Configure, compile, and install
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS}
	cp -p atk/atk.def $PKG_DIR/atk/
	make ${MAKE_PARALLEL_FLAGS}
	make install

	#Add in MS build tools again
	setup_ms_build_env_path
	
	cd "$IntDir/atk/"
	cd atk/.libs/
	$MSLIB /name:lib${Prefix}atk-1.0-0.dll /out:atk-1.0.lib /machine:$MSLibMachine /def:lib${Prefix}atk-1.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	cd ../../
	
	update_library_names_windows "lib${Prefix}atk-1.0.dll.a" "libatk-1.0.la"
fi

#openssl
#if [ ! -f "$LibDir/libcrypto.so" ]; then 
#	unpack_gzip_and_move "openssl.tar.gz" "$PKG_DIR_OPENSSL"
#	mkdir_and_move "$IntDir/openssl"
#	cd "$PKG_DIR/"
#	
#	echo Building OpenSSL...
#	cp ../mingw.bat ms/
#	cmd.exe /c "ms\mingw.bat"
#	rm -f ms/mingw.bat
#fi

#libgpg-error
if [ ! -f "$BinDir/lib${Prefix}gpg-error-0.dll" ]; then 
	unpack_bzip2_and_move "libgpg-error.tar.bz2" "$PKG_DIR_LIBGPG_ERROR"
	mkdir_and_move "$IntDir/libgpg-error"
	
	CFLAGS=$ORIG_CFLAGS
	CPPFLAGS=$ORIG_CPPFLAGS
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	
	#This file was being incorrectly linked. The script points to src/versioninfo.o when it should be 
	#src/.libs/versioninfo.o. This attempts to correct this simply by copying the .o file to the src/ dir.
	cd src
	make versioninfo.o
	cp .libs/versioninfo.o .
	cd ..
	
	#For whatever reason, CPPFLAGS is still being set. We don't want that so ensure it's blank...
	make CPPFLAGS=
	make install
	
	$MSLIB /name:lib${Prefix}gpg-error-0.dll /out:gpg-error.lib /machine:$MSLibMachine /def:src/.libs/lib${Prefix}gpg-error-0.dll.def
	
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}gpg-error.dll.a" "libgpg-error.la"
	
	reset_flags
fi

#libgcrypt
if [ ! -f "$BinDir/lib${Prefix}gcrypt-11.dll" ]; then 
	unpack_bzip2_and_move "libgcrypt.tar.bz2" "$PKG_DIR_LIBGCRYPT"
	mkdir_and_move "$IntDir/libgcrypt"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	$MSLIB /name:lib${Prefix}gcrypt-11.dll /out:gcrypt.lib /machine:$MSLibMachine /def:src/.libs/lib${Prefix}gcrypt-11.dll.def
	
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cd "$LibDir"
	rm -f "libgcrypt.def"
	
	update_library_names_windows "lib${Prefix}gcrypt.dll.a" "libgcrypt.la"
fi

#libtasn1
if [ ! -f "$BinDir/lib${Prefix}tasn1-3.dll" ]; then 
	unpack_gzip_and_move "libtasn1.tar.gz" "$PKG_DIR_LIBTASN1"
	mkdir_and_move "$IntDir/libtasn1"
	
	CFLAGS=$ORIG_CFLAGS
	CPPFLAGS=$ORIG_CPPFLAGS
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	
	reset_flags
	
	make ${MAKE_PARALLEL_FLAGS} && make install

	pexports "$BinDir/lib${Prefix}tasn1-3.dll" > in.def
	sed -e "/LIBRARY lib${Prefix}tasn1-3.dll/d" -e '/DATA/d' in.def > in-mod.def

	$MSLIB /name:lib${Prefix}tasn1-3.dll /out:tasn1.lib /machine:$MSLibMachine /def:in-mod.def
	
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cd "$LibDir"
	rm -f "libtasn1.def"
	
	update_library_names_windows "lib${Prefix}tasn1.dll.a" "libtasn1.la"
fi

#gmp
if [ ! -f "$BinDir/libgmp-10.dll" ]; then 
	unpack_bzip2_and_move "gmp.tar.bz2" "$PKG_DIR_GMP"
	mkdir_and_move "$IntDir/gmp"
	
	CFLAGS=""
	CPPFLAGS=""
	LDFLAGS=""
	ABI=32 ac_cv_sizeof_mp_limb_t=4 $PKG_DIR/configure --build=i686-pc-mingw32 --host=i686-pc-mingw32 --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS}
	make install

	reset_flags

	cd .libs/
	$dlltool -l gmp.lib -d libgmp-3.dll.def
	move_files_to_dir "*.lib" "$LibDir/"
fi

#nettle
if [ ! -f "$BinDir/libnettle-4-0.dll" ]; then 
	unpack_gzip_and_move "nettle.tar.gz" "$PKG_DIR_NETTLE"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/nettle/configure.ac.patch"
	mkdir_and_move "$IntDir/nettle"
	
	cd "$PKG_DIR/"
	autoreconf --install --force

	cd "$IntDir/nettle"
	$PKG_DIR/configure --disable-pic --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS}
	make install

	pexports "libnettle-4-0.dll" > nettle.def
	pexports "libhogweed-2-0.dll" > hogweed.def
	
	$dlltool -l nettle.lib -d nettle.def
	$dlltool -l hogweed.lib -d hogweed.def
	move_files_to_dir "*.lib" "$LibDir/"

	rm -f "$LibDir/libnettle.a"
	rm -f "$LibDir/libhogweed.a"
	
	cp -p libnettle-4-0.dll "$BinDir/"
	cp -p libhogweed-2-0.dll "$BinDir/"
fi

#gnutls
if [ ! -f "$BinDir/libgnutls-26.dll" ]; then 
	unpack_bzip2_and_move "gnutls.tar.bz2" "$PKG_DIR_GNUTLS"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/gnutls/gnutls_openssl.c.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/gnutls/openssl.h.patch"
	mkdir_and_move "$IntDir/gnutls"
	
	CFLAGS="-I$PKG_DIR/lib/includes -I$IntDir/gnutls/lib/includes $CFLAGS "
	CPPFLAGS="-I$PKG_DIR/lib/includes -I$IntDir/gnutls/lib/includes $CPPFLAGS "
	
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-guile --disable-cxx --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make
	make install

	$MSLIB /name:libgnutls-26.dll /out:gnutls.lib /machine:$MSLibMachine /def:lib/libgnutls-26.def
	$MSLIB /name:libgnutls-extra-26.dll /out:gnutls-extra.lib /machine:$MSLibMachine /def:libextra/libgnutls-extra-26.def
	$MSLIB /name:libgnutls-openssl-26.dll /out:gnutls-openssl.lib /machine:$MSLibMachine /def:libextra/libgnutls-openssl-26.def
	
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cd "$BinDir" && remove_files_from_dir "libgnutls-*.def"
	
	reset_flags
fi

#curl
if [ ! -f "$BinDir/libcurl-4.dll" ]; then 
	unpack_bzip2_and_move "curl.tar.bz2" "$PKG_DIR_CURL"
	mkdir_and_move "$IntDir/curl"

	LDFLAGS="$LDFLAGS -ltasn1 -lz -lgcrypt -lgpg-error"
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --with-gnutls --enable-optimize --disable-curldebug --disable-debug --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd "lib/.libs"
	pexports "libcurl-4.dll" > in.def
	sed -e '/LIBRARY libcurl/d' in.def > in-mod.def
	
	$MSLIB /name:libcurl-4.dll /out:curl.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#soup
if [ ! -f "$BinDir/libsoup-2.4-1.dll" ]; then 
	unpack_bzip2_and_move "libsoup.tar.bz2" "$PKG_DIR_LIBSOUP"
	mkdir_and_move "$IntDir/libsoup"

	#TODO: Check if this is no longer the case. Bug reported: https://bugzilla.gnome.org/show_bug.cgi?id=606455
	#libsoup isn't outputting the correct exported symbols, so update Makefile.am so libtool will pick it up
	#What we want, essentially, is this: 
	#	libsoup_2_4_la_LDFLAGS = \
	#		-export-symbols-regex '^(soup|_soup|soup_|_SOUP_METHOD_|SOUP_METHOD_).*' \
	#		-version-info $(SOUP_CURRENT):$(SOUP_REVISION):$(SOUP_AGE) -no-undefined
	cd "$PKG_DIR/"
	change_key "libsoup" "Makefile.in" "libsoup_2_4_la_LDFLAGS" "-export-symbols-regex\ \'\^\(soup\|_soup\|soup_\|_SOUP_METHOD\|SOUP_METHOD_\)\.\*\'\ \\\\"

	#Proceed normally
	
	cd "$IntDir/libsoup"
	CPPFLAGS="$CPPFLAGS -DIN_LIBXML"
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-silent-rules --enable-ssl --enable-debug=no --disable-more-warnings --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} 

	make install

	cd "libsoup/.libs"
	pexports "$BinDir/libsoup-2.4-1.dll" > in.def
	sed -e '/LIBRARY libsoup/d' in.def > in-mod.def
	
	$MSLIB /name:libsoup-2.4-1.dll /out:soup-2.4.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#neon
if [ ! -f "$BinDir/libneon-27.dll" ]; then 
	unpack_gzip_and_move "neon.tar.gz" "$PKG_DIR_NEON"
	mkdir_and_move "$IntDir/neon"
	
	#Augment the printf formats configure looks for.
	#It doesn't know to use %I64d to print a long long
	sed -e 's/for str in d ld lld/for str in d ld lld I64d/g' $PKG_DIR/configure > $PKG_DIR/configure.tmp
	mv $PKG_DIR/configure.tmp $PKG_DIR/configure

	ac_cv_type_socklen_t=yes lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --with-ssl=gnutls --disable-debug --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make
	make install

	cd "src/.libs"
	pexports "$BinDir/libneon-27.dll" | sed "s/^_//" > in.def
	sed -e '/LIBRARY libneon-27.dll/d' in.def > in-mod.def
	$MSLIB /name:libneon-27.dll /out:neon.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#freetype
if [ ! -f "$BinDir/lib${Prefix}freetype-6.dll" ]; then 
	unpack_bzip2_and_move "freetype.tar.bz2" "$PKG_DIR_FREETYPE"
	mkdir_and_move "$IntDir/freetype"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cp -p .libs/lib${Prefix}freetype.dll.a "$LibDir/freetype.lib"
	
	update_library_names_windows "lib${Prefix}freetype.dll.a" "libfreetype.la"
fi

#fontconfig
if [ ! -f "$BinDir/lib${Prefix}fontconfig-1.dll" ]; then 
	unpack_gzip_and_move "fontconfig.tar.gz" "$PKG_DIR_FONTCONFIG"
	mkdir_and_move "$IntDir/fontconfig"

	reset_path
	
	$PKG_DIR/configure --disable-static --disable-docs --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install RUN_FC_CACHE_TEST=false
	
	setup_ms_build_env_path

	cp -p "fontconfig.pc" "$LibDir/pkgconfig/"
	
	cd "$IntDir/fontconfig/src/.libs"
	
	sed -e '/LIBRARY/d' lib${Prefix}fontconfig-1.dll.def > in-mod.def
#	$dlltool --dllname lib${Prefix}fontconfig-1.dll -d "in-mod.def" -l lib${Prefix}fontconfig.dll.a
#	cp -p "lib${Prefix}fontconfig.dll.a" "$LibDir/"
	$MSLIB /name:lib${Prefix}fontconfig-1.dll /out:fontconfig.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
	
	update_library_names_windows "lib${Prefix}fontconfig.dll.a" "libfontconfig.la"
	
	echo "<OSSBuild>: Please ignore install errors"
fi

#pixman
if [ ! -f "$BinDir/libpixman-1-0.dll" ]; then 
	unpack_gzip_and_move "pixman.tar.gz" "$PKG_DIR_PIXMAN"
	mkdir_and_move "$IntDir/pixman"
	
	ac_cv_func_mprotect=no ac_cv_func_getpagesize=no $PKG_DIR/configure --disable-gtk --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd "$LIBRARIES_PATCH_DIR/pixman/"
	$MSLIB /name:libpixman-1-0.dll /out:pixman.lib /machine:$MSLibMachine /def:pixman.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
fi

#cairo
if [ ! -f "$BinDir/libcairo-2.dll" ]; then 
	unpack_gzip_and_move "cairo.tar.gz" "$PKG_DIR_CAIRO"
	mkdir_and_move "$IntDir/cairo"
	
	CFLAGS="$CFLAGS -D ffs=__builtin_ffs -D CAIRO_HAS_WIN32_SURFACE -D CAIRO_HAS_WIN32_FONT -Wl,-lpthreadGC2"
	lt_cv_deplibs_check_method=pass_all ax_cv_c_float_words_bigendian=no $PKG_DIR/configure --enable-gl=no --enable-xlib=auto --enable-xlib-xrender=auto --enable-png=yes --enable-ft=yes --enable-pdf=yes --enable-svg=yes --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS}
	make install

	cd "$IntDir/cairo/src/.libs/"
	sed -e '/LIBRARY/d' libcairo-2.dll.def > in-mod.def
	##$dlltool --dllname libcairo-2.dll -d "in-mod.def" -l libcairo.dll.a
	##cp -p "libcairo.dll.a" "$LibDir/"
	$MSLIB /name:libcairo-2.dll /out:cairo.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cd "$IntDir/cairo/util/cairo-script/.libs/"
	pexports "libcairo-script-interpreter-2.dll" | sed "s/^_//" > in.def
	sed -e '/LIBRARY/d' in.def > in-mod.def
	$MSLIB /name:libcairo-script-interpreter-2.dll /out:cairo-script-interpreter.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cd "$IntDir/cairo/util/cairo-gobject/.libs/"
	pexports "libcairo-gobject-2.dll" | sed "s/^_//" > in.def
	sed -e '/LIBRARY/d' in.def > in-mod.def
	$MSLIB /name:libcairo-gobject-2.dll /out:cairo-gobject.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#pango
if [ ! -f "$BinDir/lib${Prefix}pango-1.0-0.dll" ]; then 
	unpack_bzip2_and_move "pango.tar.bz2" "$PKG_DIR_PANGO"
	mkdir_and_move "$IntDir/pango"
	
	#Need to get rid of MS build tools b/c the makefile call is incorrectly passing it msys-style paths.
	reset_path
	
	$PKG_DIR/configure --with-included-modules --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	if [ -e "/mingw/lib/libws2_32.la" ]; then 
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libgdi32.la /mingw/lib/libmsimg32.la" "pango/opentype"
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libgdi32.la /mingw/lib/libmsimg32.la" "pango-view"
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libgdi32.la /mingw/lib/libmsimg32.la" "examples"
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libgdi32.la /mingw/lib/libmsimg32.la" "pango"
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libgdi32.la /mingw/lib/libmsimg32.la" "tests"
	fi
	
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	if [ -e "$LibDir/libpango-1.0-0.dll" ]; then
		mv "$LibDir/libpango-1.0-0.dll" "$BinDir/"
		mv "$LibDir/libpangoft2-1.0-0.dll" "$BinDir/"
		mv "$LibDir/libpangowin32-1.0-0.dll" "$BinDir/"
		mv "$LibDir/libpangocairo-1.0-0.dll" "$BinDir/"
	fi
	if [ -e "$LibDir/libpango-1.0.lib" ]; then
		rm -f "$LibDir/libpango-1.0.lib"
		rm -f "$LibDir/libpangocairo-1.0.lib"
		rm -f "$LibDir/libpangoft2-1.0.lib"
		rm -f "$LibDir/libpangowin32-1.0.lib"
	fi

	#Add in MS build tools again
	setup_ms_build_env_path

	cd "$IntDir/pango/pango/.libs/"
	$MSLIB /name:lib${Prefix}pango-1.0-0.dll /out:pango-1.0.lib /machine:$MSLibMachine /def:lib${Prefix}pango-1.0-0.dll.def
	$MSLIB /name:lib${Prefix}pangoft2-1.0-0.dll /out:pangoft2-1.0.lib /machine:$MSLibMachine /def:lib${Prefix}pangoft2-1.0-0.dll.def
	$MSLIB /name:lib${Prefix}pangowin32-1.0-0.dll /out:pangowin32-1.0.lib /machine:$MSLibMachine /def:lib${Prefix}pangowin32-1.0-0.dll.def
	$MSLIB /name:lib${Prefix}pangocairo-1.0-0.dll /out:pangocairo-1.0.lib /machine:$MSLibMachine /def:lib${Prefix}pangocairo-1.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cp -p "$LibDir/pango-1.0.lib" "$LibDir/libpango-1.0.dll.a"
	cp -p "$LibDir/pangoft2-1.0.lib" "$LibDir/libpangoft2-1.0.dll.a"
	cp -p "$LibDir/pangowin32-1.0.lib" "$LibDir/libpangowin32-1.0.dll.a"
	cp -p "$LibDir/pangocairo-1.0.lib" "$LibDir/libpangocairo-1.0.dll.a"
	
	change_key "$LibDir" "libpango-1.0.la" "library_names" "'libpango-1.0.dll.a'"
	change_key "$LibDir" "libpangoft2-1.0.la" "library_names" "'libpangoft2-1.0.dll.a'"
	change_key "$LibDir" "libpangowin32-1.0.la" "library_names" "'libpangowin32-1.0.dll.a'"
	change_key "$LibDir" "libpangocairo-1.0.la" "library_names" "'libpangocairo-1.0.dll.a'"
	
	update_library_names_windows "lib${Prefix}pango-1.0.dll.a" "libpango-1.0.la"
	update_library_names_windows "lib${Prefix}pangoft2-1.0.dll.a" "libpangoft2-1.0.la"
	update_library_names_windows "lib${Prefix}pangowin32-1.0.dll.a" "libpangowin32-1.0.la"
	update_library_names_windows "lib${Prefix}pangocairo-1.0.dll.a" "libpangocairo-1.0.la"
	
	cd "$LibDir" && remove_files_from_dir "pango*.def"
fi

#libffi
#Preparing for possible gobject-introspection in the future (it would go after this and before gkd-pixbuf)
if [ ! -f "$BinDir/libffi-5.dll" ]; then
	unpack_gzip_and_move "libffi.tar.gz" "$PKG_DIR_LIBFFI"
	mkdir_and_move "$IntDir/libffi"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS}
	make install
	
	mv "$LibDir/libffi-3.0.10rc0/include/*" "$IncludeDir/"
	rm -rf "$LibDir/libffi-3.0.10rc0/"

	cd .libs/
	pexports "libffi-5.dll" | sed -e "s/^_//" > in.def
	$MSLIB /name:libffi-5.dll /out:ffi.lib /machine:$MSLibMachine /def:in.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
fi

#gobject-introspection
#Check out patch here: https://bugzilla.gnome.org/show_bug.cgi?id=620566

#gdk-pixbuf
if [ ! -f "$BinDir/libgdk_pixbuf-2.0-0.dll" ]; then 
	unpack_bzip2_and_move "gdk-pixbuf.tar.bz2" "$PKG_DIR_GDKPIXBUF"
	mkdir_and_move "$IntDir/gdk-pixbuf"
	
	#Clear MS tools
	reset_path
	
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --without-libjasper --with-included-loaders --disable-debug --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install

	#Put MS tools back in
	setup_ms_build_env_path

	cd "$IntDir/gdk-pixbuf"
	mv "$LibDir/gdk_pixbuf-2.0.def" .
	$MSLIB /name:libgdk_pixbuf-2.0-0.dll /out:gdk_pixbuf-2.0.lib /machine:$MSLibMachine /def:gdk_pixbuf-2.0.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
fi

#gtk+
if [ ! -f "$BinDir/libgtk-win32-2.0-0.dll" ]; then 
	unpack_bzip2_and_move "gtk+.tar.bz2" "$PKG_DIR_GTKPLUS"
	mkdir_and_move "$IntDir/gtkplus"
	
	#Configure, compile, and install
	
	#Need to get rid of MS build tools b/c the makefile call is linking to the wrong dll
	reset_path
	
	CFLAGS="$CFLAGS -DMINGW64"
	LDFLAGS="$LDFLAGS -luuid -lstrmiids"
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --with-gdktarget=win32 --disable-modules --with-included-immodules --disable-debug --disable-gtk-doc --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir	
	
	#The resource files are using msys/mingw-style paths and windres is having trouble finding the icons
	cd gdk/win32/rc/
	cp $PKG_DIR/gdk/win32/rc/gdk.rc .
	cp $PKG_DIR/gdk/win32/rc/gtk.ico .
	$windres gdk.rc gdk-win32-res.o
	change_key "." "Makefile" "WINDRES" "true"
	cd ../../../
	
	make ${MAKE_PARALLEL_FLAGS}
	make install

	#Add in MS build tools again
	setup_ms_build_env_path
	
	grep -v -E 'Automatically generated|Created by|LoaderDir =' <$OutDir/etc/gtk-2.0/gdk-pixbuf.loaders >$OutDir/etc/gtk-2.0/gdk-pixbuf.loaders.temp
	mv $OutDir/etc/gtk-2.0/gdk-pixbuf.loaders.temp $OutDir/etc/gtk-2.0/gdk-pixbuf.loaders
	grep -v -E 'Automatically generated|Created by|ModulesPath =' <$OutDir/etc/gtk-2.0/gtk.immodules >$OutDir/etc/gtk-2.0/gtk.immodules.temp 
	mv $OutDir/etc/gtk-2.0/gtk.immodules.temp $OutDir/etc/gtk-2.0/gtk.immodules
	
	cp -p "$LibDir/gailutil.def" .
	cp -p "$LibDir/gdk-win32-2.0.def" .
	cp -p "$LibDir/gtk-win32-2.0.def" .
	$MSLIB /name:libgailutil-18.dll /out:gailutil.lib /machine:$MSLibMachine /def:gailutil.def
	$MSLIB /name:libgdk-win32-2.0-0.dll /out:gdk-win32-2.0.lib /machine:$MSLibMachine /def:gdk-win32-2.0.def
	$MSLIB /name:libgtk-win32-2.0-0.dll /out:gtk-win32-2.0.lib /machine:$MSLibMachine /def:gtk-win32-2.0.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	reset_flags
	
	cp -p "$LibDir/gtk-2.0/include/gdkconfig.h" "$IncludeDir/gtk-2.0/"
	$rm -rf "$LibDir/gtk-2.0/include/"
fi

#gtkglarea
if [ ! -f "$BinDir/lib${Prefix}gtkgl-2.0-1.dll" ]; then 
	unpack_bzip2_and_move "gtkglarea.tar.bz2" "$PKG_DIR_GTKGLAREA"
	mkdir_and_move "$IntDir/gtkglarea"
	
	#Need to get rid of MS build tools b/c the makefile call is linking to the wrong dll
	reset_path
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	cp -p $PKG_DIR/gtkgl/gtkgl.def gtkgl/
	make ${MAKE_PARALLEL_FLAGS}
	cp -p gtkgl/.libs/lib${Prefix}gtkgl-2.0.dll.a gtkgl/.libs/libgtkgl-2.0.dll.a
	make install
	
	#Add in MS build tools again
	setup_ms_build_env_path
	
	cd "$IntDir/gtkglarea"
	cd gtkgl/.libs/
	$MSLIB /name:lib${Prefix}gtkgl-2.0-1.dll /out:gtkgl-2.0.lib /machine:$MSLibMachine /def:lib${Prefix}gtkgl-2.0-1.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	cd ../../
	
	update_library_names_windows "lib${Prefix}gtkgl-2.0.dll.a" "libgtkgl-2.0.la"
fi

if [ -e "$PERL_BIN_DIR" ]; then 
	#libcroco
	if [ ! -f "$BinDir/lib${Prefix}croco-0.6-3.dll" ]; then 
		unpack_bzip2_and_move "libcroco.tar.bz2" "$PKG_DIR_LIBCROCO"
		mkdir_and_move "$IntDir/libcroco"
		
		cd "$PKG_DIR/"
		change_key "src" "Makefile.in" "libcroco_0_6_la_LDFLAGS" "-version-info\ @LIBCROCO_VERSION_INFO@\ -export-symbols-regex\ \'\^\(cr_)\.\*\'\ \\\\"

		CPPFLAGS="$CPPFLAGS -DIN_LIBXML"
		$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
		change_libname_spec
		make ${MAKE_PARALLEL_FLAGS} && make install
	
		reset_flags
		
		cd src/.libs
		$MSLIB /name:lib${Prefix}croco-0.6-3.dll /out:croco-0.6.lib /machine:$MSLibMachine /def:lib${Prefix}croco-0.6-3.dll.def
		move_files_to_dir "*.exp *.lib" "$LibDir/"
		cd ../../
		
		update_library_names_windows "lib${Prefix}croco-0.6.dll.a" "libcroco-0.6.la"
		
		#For make test
		cd csslint/.libs/
		cp -p lib${Prefix}croco-0.6-3.dll ../../csslint/.libs/
		cp -p "$BinDir/lib${Prefix}xml2-2.dll" .
		cp -p "$BinDir/lib${Prefix}glib-2.0-0.dll" .
		cp -p "$BinDir/*${IconvPrefix}iconv*.dll" .
		cp -p "$BinDir/${ZlibPrefix}z.dll" .
		cd ../../
		
		cd tests/.libs/
		cp -p ../../csslint/.libs/lib${Prefix}croco-0.6-3.dll .
		cp -p "$BinDir/lib${Prefix}xml2-2.dll" .
		cp -p "$BinDir/lib${Prefix}glib-2.0-0.dll" .
		cp -p "$BinDir/*${IconvPrefix}iconv*.dll" .
		cp -p "$BinDir/${ZlibPrefix}z.dll" .
		cd ../
	fi

	reset_path
	setup_ms_build_env_path
	export PATH=$PERL_BIN_DIR:$PATH
	#intltool
	if [ ! -f "$BinDir/intltool-merge" ]; then 
		unpack_bzip2_and_move "intltool.tar.bz2" "$PKG_DIR_INTLTOOL"
		mkdir_and_move "$IntDir/intltool"
		
		$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
		make ${MAKE_PARALLEL_FLAGS} && make install
	fi

	#libgsf
	if [ ! -f "$BinDir/lib${Prefix}gsf-1-114.dll" ]; then 
		unpack_bzip2_and_move "libgsf.tar.bz2" "$PKG_DIR_LIBGSF"
		mkdir_and_move "$IntDir/libgsf"
		
		CPPFLAGS="$CPPFLAGS -DIN_LIBXML"
		$PKG_DIR/configure --without-python --disable-gtk-doc --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
		change_libname_spec
		make ${MAKE_PARALLEL_FLAGS} && make install
	
		reset_flags
		
		cd gsf/.libs/
		$MSLIB /name:lib${Prefix}gsf-1-114.dll /out:gsf-1.lib /machine:$MSLibMachine /def:lib${Prefix}gsf-1-114.dll.def
		move_files_to_dir "*.exp *.lib" "$LibDir/"
		cd ../../
		
		cd gsf-win32/.libs/
		$MSLIB /name:lib${Prefix}gsf-win32-1-114.dll /out:gsf-win32-1.lib /machine:$MSLibMachine /def:lib${Prefix}gsf-win32-1-114.dll.def
		move_files_to_dir "*.exp *.lib" "$LibDir/"
		cd ../../
		
		update_library_names_windows "lib${Prefix}gsf-1.dll.a" "libgsf-1.la"
		update_library_names_windows "lib${Prefix}gsf-win32-1.dll.a" "libgsf-win32-1.la"
	fi
	reset_path
	setup_ms_build_env_path

	#librsvg
	if [ ! -f "$BinDir/lib${Prefix}rsvg-2-2.dll" ]; then 
		unpack_bzip2_and_move "librsvg.tar.bz2" "$PKG_DIR_LIBRSVG"
		mkdir_and_move "$IntDir/librsvg"
		
		$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
		change_libname_spec
		
		make ${MAKE_PARALLEL_FLAGS} && make install
		
		cd .libs/
		$MSLIB /name:lib${Prefix}rsvg-2-2.dll /out:rsvg-2.lib /machine:$MSLibMachine /def:lib${Prefix}rsvg-2-2.dll.def
		move_files_to_dir "*.exp *.lib" "$LibDir/"
		cd ../
	fi
fi

#sdl
if [ ! -f "$BinDir/SDL.dll" ]; then 
	unpack_gzip_and_move "sdl.tar.gz" "$PKG_DIR_SDL"
	mkdir_and_move "$IntDir/sdl"
	
	cd "$PKG_DIR/"
	./autogen.sh

	cd "$IntDir/sdl/"
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir 
	make ${MAKE_PARALLEL_FLAGS}
	make install

	cp $PKG_DIR/include/SDL_config.h.default $IncludeDir/SDL/SDL_config.h
	cp $PKG_DIR/include/SDL_config_win32.h $IncludeDir/SDL
	
	cd build/.libs
	
	pexports "$BinDir/SDL.dll" | sed "s/^_//" > in.def
	sed -e '/LIBRARY SDL.dll/d' in.def > in-mod.def
	$MSLIB /name:SDL.dll /out:sdl.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	reset_flags
fi

#SDL_ttf
if [ ! -f "$BinDir/SDL_ttf.dll" ]; then 
	unpack_gzip_and_move "sdl_ttf.tar.gz" "$PKG_DIR_SDLTTF"
	mkdir_and_move "$IntDir/sdl_ttf"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd .libs/
	pexports "$BinDir/SDL_ttf.dll" | sed "s/^_//" > in.def
	sed -e '/LIBRARY SDL_ttf.dll/d' in.def > in-mod.def
	$MSLIB /name:SDL_ttf.dll /out:sdl_ttf.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	cd ..
fi

#libogg
if [ ! -f "$BinDir/lib${Prefix}ogg-0.dll" ]; then 
	unpack_gzip_and_move "libogg.tar.gz" "$PKG_DIR_LIBOGG"
	mkdir_and_move "$IntDir/libogg"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	copy_files_to_dir "$PKG_DIR/win32/ogg.def" .
	$MSLIB /name:lib${Prefix}ogg-0.dll /out:ogg.lib /machine:$MSLibMachine /def:ogg.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}ogg.dll.a" "libogg.la"
fi

#libflac
if [ ! -f "$BinDir/lib${Prefix}FLAC-8.dll" ]; then 
	unpack_gzip_and_move "flac.tar.gz" "$PKG_DIR_FLAC"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/flac/flac-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/flac/flac-size-t-max.patch"
	mkdir_and_move "$IntDir/flac"	
	
	#'make install' tries to install some files from this folder but doesn't create it
	mkdir -p 'doc/html/api'
	touch 'doc/html/api/null'
	
	#Configure, compile, and install
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir --disable-doxygen-docs --disable-cpplibs LDFLAGS="$LDFLAGS -no-undefined -lws2_32" 
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS}
	make install
	
	pexports "$BinDir/lib${Prefix}FLAC-8.dll" > in.def
	sed -e '/LIBRARY lib${Prefix}flac/d' -e 's/DATA//g' in.def > in-mod.def
	$MSLIB /name:lib${Prefix}FLAC-8.dll /out:flac.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir"
	
	update_library_names_windows "lib${Prefix}flac.dll.a" "libflac.la"
	
	reset_flags
fi

#libvorbis
if [ ! -f "$BinDir/lib${Prefix}vorbis-0.dll" ]; then 
	unpack_bzip2_and_move "libvorbis.tar.bz2" "$PKG_DIR_LIBVORBIS"
	mkdir_and_move "$IntDir/libvorbis"
	
	LDFLAGS="$LDFLAGS -logg"
	$PKG_DIR/configure --with-ogg-libraries=$LibDir --with-ogg-includes=$IncludeDir --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	update_library_names_windows "lib${Prefix}vorbis.dll.a" "libvorbis.la"
	
	#Yeah, we're calling this twice b/c all the object files are compiled for all the libs, 
	#but they're not all being linked and installed b/c it can't find -lvorbis. Calling 
	#make/make install twice seems to solve it. But it's a hack.
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	copy_files_to_dir "$PKG_DIR/win32/*.def" .
	sed '/vorbis_encode_*/d' vorbis.def > vorbis-mod.def
	$MSLIB /name:lib${Prefix}vorbis-0.dll /out:vorbis.lib /machine:$MSLibMachine /def:vorbis-mod.def
	$MSLIB /name:lib${Prefix}vorbisenc-2.dll /out:vorbisenc.lib /machine:$MSLibMachine /def:vorbisenc.def
	$MSLIB /name:lib${Prefix}vorbisfile-3.dll /out:vorbisfile.lib /machine:$MSLibMachine /def:vorbisfile.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
	
	update_library_names_windows "lib${Prefix}vorbis.dll.a" "libvorbis.la"
	update_library_names_windows "lib${Prefix}vorbisenc.dll.a" "libvorbisenc.la"
	update_library_names_windows "lib${Prefix}vorbisfile.dll.a" "libvorbisfile.la"
fi

#libcelt
#TODO: Fix this!
if [ ! -f "$BinDir/lib${Prefix}celt-0.dll" ]; then 
	unpack_gzip_and_move "libcelt.tar.gz" "$PKG_DIR_LIBCELT"
	mkdir_and_move "$IntDir/libcelt"
	
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir

	cd "libcelt"

	#This will fail to produce the dll for some reason. So we build all the object files first and then create the shared library ourselves.
	make libcelt0.la
	$gcc --link -shared -o .libs/libcelt-0.dll -Wl,--output-def=libcelt.def -Wl,--out-implib=.libs/libcelt0.dll.a -std=gnu99 $LDFLAGS \
		.libs/bands.o \
		.libs/celt.o \
		.libs/cwrs.o \
		.libs/entcode.o \
		.libs/entdec.o \
		.libs/entenc.o \
		.libs/header.o \
		.libs/kiss_fft.o \
		.libs/laplace.o \
		.libs/mdct.o \
		.libs/modes.o \
		.libs/pitch.o \
		.libs/plc.o \
		.libs/quant_bands.o \
		.libs/rangedec.o \
		.libs/rangeenc.o \
		.libs/rate.o \
		.libs/vq.o
	rm libcelt0.la
	rm .libs/libcelt0.la
	rm .libs/libcelt0.lai
	rm .libs/libcelt0.a
	echo -en "# Generated by ossbuild - GNU libtool 1.5.22 (1.1220.2.365 2005/12/18 22:14:06)\n" > libcelt0.la
	echo -en "dlname='lib${Prefix}celt-0.dll'\n" >> libcelt0.la
	echo -en "library_names='lib${Prefix}celt0.dll.a'\n" >> libcelt0.la
	echo -en "old_library=''\n" >> libcelt0.la
	echo -en "inherited_linker_flags=''\n" >> libcelt0.la
	echo -en "dependency_libs=''\n" >> libcelt0.la
	echo -en "weak_library_names=''\n" >> libcelt0.la
	echo -en "current=0\n" >> libcelt0.la
	echo -en "age=0\n" >> libcelt0.la
	echo -en "revision=0\n" >> libcelt0.la
	echo -en "installed=no\n" >> libcelt0.la
	echo -en "shouldnotlink=no\n" >> libcelt0.la
	echo -en "dlopen=''\n" >> libcelt0.la
	echo -en "dlpreopen=''\n" >> libcelt0.la
	echo -en "libdir='$LibDir'\n" >> libcelt0.la
	cp -p libcelt0.la .libs/
	cd .libs/
	echo -en "# Generated by ossbuild - GNU libtool 1.5.22 (1.1220.2.365 2005/12/18 22:14:06)\n" > libcelt0.lai
	echo -en "dlname='../bin/lib${Prefix}celt-0.dll'\n" >> libcelt0.lai
	echo -en "library_names='lib${Prefix}celt0.dll.a'\n" >> libcelt0.lai
	echo -en "old_library=''\n" >> libcelt0.lai
	echo -en "inherited_linker_flags=''\n" >> libcelt0.lai
	echo -en "dependency_libs='-L$LibDir -lm'\n" >> libcelt0.lai
	echo -en "weak_library_names=''\n" >> libcelt0.lai
	echo -en "current=0\n" >> libcelt0.lai
	echo -en "age=0\n" >> libcelt0.lai
	echo -en "revision=0\n" >> libcelt0.lai
	echo -en "installed=yes\n" >> libcelt0.lai
	echo -en "shouldnotlink=no\n" >> libcelt0.lai
	echo -en "dlopen=''\n" >> libcelt0.lai
	echo -en "dlpreopen=''\n" >> libcelt0.lai
	echo -en "libdir='$LibDir'\n" >> libcelt0.lai
	cd ..
	make
	make install
	
	sed -e 's/main//g' -e 's/DATA//g' -e 's/@[0-9]*//g' libcelt.def > in.def
	$MSLIB /name:libcelt-0.dll /out:celt0.lib /machine:$MSLibMachine /def:in.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
	
	update_library_names_windows "lib${Prefix}celt0.dll.a" "libcelt0.la"
fi

#libtheora
if [ ! -f "$BinDir/lib${Prefix}theora-0.dll" ]; then 
	unpack_bzip2_and_move "libtheora.tar.bz2" "$PKG_DIR_LIBTHEORA"
	mkdir_and_move "$IntDir/libtheora"
	
	$PKG_DIR/configure --with-vorbis=$BinDir --with-vorbis-libraries=$LibDir --with-vorbis-includes=$IncludeDir --with-ogg=$BinDir --with-ogg-libraries=$LibDir --with-ogg-includes=$IncludeDir --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make 
	make install
	make
	make install
	
	copy_files_to_dir "$LIBRARIES_PATCH_DIR/theora/*def" .
	copy_files_to_dir "lib/.libs/*.def" .
	flip -d libtheora.def
	sed -e '/LIBRARY	libtheora/d' libtheora.def > libtheora-mod.def
	$MSLIB /name:lib${Prefix}theora-0.dll /out:theora.lib /machine:$MSLibMachine /def:libtheora-mod.def
	$MSLIB /name:lib${Prefix}theoradec-1.dll /out:theoradec.lib /machine:$MSLibMachine /def:lib${Prefix}theoradec-1.dll.def
	$MSLIB /name:lib${Prefix}theoraenc-1.dll /out:theoraenc.lib /machine:$MSLibMachine /def:lib${Prefix}theoraenc-1.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}theora.dll.a" "libtheora.la"
	update_library_names_windows "lib${Prefix}theoradec.dll.a" "libtheoradec.la"
	update_library_names_windows "lib${Prefix}theoraenc.dll.a" "libtheoraenc.la"
fi

#libmms
if [ ! -f "$BinDir/lib${Prefix}mms-0.dll" ]; then 
	unpack_bzip2_and_move "libmms.tar.bz2" "$PKG_DIR_LIBMMS"
	mkdir_and_move "$IntDir/libmms"
	
	CFLAGS="$CFLAGS -D LIBMMS_HAVE_64BIT_OFF_T"
	LDFLAGS="$LDFLAGS -lwsock32 -lglib-2.0 -lgobject-2.0"
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	if [ -e "/mingw/lib/libws2_32.la" ]; then 
		copy_files_to_dir "/mingw/lib/libws2_32.la /mingw/lib/libole32.la /mingw/lib/libwsock32.la" "src"
	fi
	make && make install
	
	copy_files_to_dir "$LIBRARIES_PATCH_DIR/libmms/*.def" .
	$MSLIB /name:lib${Prefix}mms-0.dll /out:mms.lib /machine:$MSLibMachine /def:libmms.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
	
	update_library_names_windows "lib${Prefix}mms.dll.a" "libmms.la"
fi

#x264
if [ ! -f "$BinDir/x264.exe" ]; then 
	unpack_bzip2_and_move "x264.tar.bz2" "$PKG_DIR_X264"
	mkdir_and_move "$IntDir/x264"
	
	PATH=$PATH:$TOOLS_DIR
	
	cd "$PKG_DIR/"
	CFLAGS=""
	CPPFLAGS=""
	LDFLAGS="-Wl,--exclude-libs,libpthreadGC2.dll.a -Wl,--exclude-libs,pthreadGC2.lib"
	ORIG_LDFLAGS="$ORIG_LDFLAGS -Wl,--exclude-libs=libintl.a -Wl,--add-stdcall-alias"
	./configure --disable-gpac --enable-shared --host=$Host --prefix=$InstallDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	API=$(grep '#define X264_BUILD' < x264.h | cut -f 3 -d ' ')
	
	#Download example y4m for use in profiling and selecting the best CPU instruction set
	wget http://samples.mplayerhq.hu/yuv4mpeg2/example.y4m.bz2
	bunzip2 -d -f "example.y4m.bz2"
	
	#Build using the profiler
	make fprofiled VIDS="example.y4m"
	#make
	make install
	
	reset_flags
	
	reset_path
	setup_ms_build_env_path

	cd "$IntDir/x264"
	rm -rf "$LibDir/libx264.a"
	pexports "$BinDir/lib${Prefix}x264-$API.dll" | sed "s/^_//" > in.def
	sed -e 's/DATA//g' in.def > in-mod.def
	$MSLIB /name:libx264-$API.dll /out:x264.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}x264.dll.a" "libx264.la"
fi

#libspeex
if [ ! -f "$BinDir/libspeex-1.dll" ]; then 
	unpack_gzip_and_move "speex.tar.gz" "$PKG_DIR_LIBSPEEX"
	mkdir_and_move "$IntDir/libspeex"

	cd "$IntDir/libspeex"
	mkdir -p src/no/lib/
	touch src/no/lib/
	
	#TODO: Reevaluate if sse has been fixed b/t gcc and msvc (stack alignment issue?)
	#-fno-toplevel-reorder instead of -fno-unit-at-a-time?
	CFLAGS='-fno-toplevel-reorder -fno-unit-at-a-time -fno-inline-functions ' \
	LDFLAGS='' \
	$PKG_DIR/configure --with-fft=smallft --without-pic --disable-sse --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make
	make install

	copy_files_to_dir "$PKG_DIR/win32/*.def" .
	
	#Remove the explicit library declaration since it overrides our commandline one
	sed '/LIBRARY libspeex/d' libspeex.def > libspeex-mod.def
	sed '/LIBRARY libspeexdsp/d' libspeexdsp.def > libspeexdsp-mod.def
	
	#Add some missing functions from our lib to the def file
	echo speex_nb_mode >> libspeex-mod.def
	echo speex_uwb_mode >> libspeex-mod.def
	echo speex_wb_mode >> libspeex-mod.def
	echo speex_header_free >> libspeex-mod.def
	echo speex_mode_list >> libspeex-mod.def
	
	$MSLIB /name:libspeex-1.dll /out:speex.lib /machine:$MSLibMachine /def:libspeex-mod.def
	$MSLIB /name:libspeexdsp-1.dll /out:speexdsp.lib /machine:$MSLibMachine /def:libspeexdsp-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	cp -p "libspeex/.libs/libspeexdsp.dll.a" "$LibDir"
	
	reset_flags
fi

#TODO: Remove patch if schroedinger/Makefile.am has been fixed
#libschroedinger (dirac support)
if [ ! -f "$BinDir/libschroedinger-1.0-0.dll" ]; then 
	unpack_gzip_and_move "schroedinger.tar.gz" "$PKG_DIR_LIBSCHROEDINGER"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/schroedinger/schroedinger.patch"
	mkdir_and_move "$IntDir/libschroedinger"
	
	cd "$PKG_DIR/"
	autoreconf -i -f

	cd "$IntDir/libschroedinger"
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --with-thread=win32 --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd "schroedinger/.libs"
	$MSLIB /name:libschroedinger-1.0-0.dll /out:schroedinger-1.0.lib /machine:$MSLibMachine /def:libschroedinger-1.0-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#Not supported at this time!
#taglib
#if [ ! -f "$BinDir/taglib.dll" ]; then 
#	mkdir_and_move "$IntDir/taglib"
#	
#	cd $LIBRARIES_DIR/TagLib/Source/
#	autoreconf -i -f
#	
#	cd "$IntDir/taglib"
#	$LIBRARIES_DIR/TagLib/Source/configure --disable-nmcheck --disable-final --disable-coverage --disable-static --enable-shared --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
#	
#	#Overwrite the auto-generated libtool (it's old) w/ a newer one that properly handles dll.a files
#	cp -p "$LIBRARIES_DIR/TagLib/libtool-windows" ./libtool
#	make && make install
#	
#	#copy_files_to_dir "$LIBRARIES_DIR/TagLib/Source/win32/*.def" .
#	#$MSLIB /out:libspeex.lib /machine:$MSLibMachine /def:libspeex.def
#	#move_files_to_dir "*.exp *.lib" "$LibDir/"
#fi

#mp3lame
if [ ! -f "$BinDir/lib${Prefix}mp3lame-0.dll" ]; then 
	unpack_gzip_and_move "lame.tar.gz" "$PKG_DIR_MP3LAME"
	mkdir_and_move "$IntDir/mp3lame"
	
	$PKG_DIR/configure --enable-expopt=no --enable-debug=no --disable-brhist -disable-frontend --enable-nasm --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_libname_spec
	make && make install
	
	pexports "$BinDir/lib${Prefix}mp3lame-0.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libmp3lame-0.dll/d' in.def > in-mod.def
	$MSLIB /name:lib${Prefix}mp3lame-0.dll /out:mp3lame.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	update_library_names_windows "lib${Prefix}mp3lame.dll.a" "libmp3lame.la"
fi

#libsndfile
if [ ! -f "$BinDir/libsndfile-1.dll" ]; then 
	unpack_gzip_and_move "libsndfile.tar.gz" "$PKG_DIR_LIBSNDFILE"
	mkdir_and_move "$IntDir/libsndfile"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make && make install
	
	pexports "$BinDir/libsndfile-1.dll" > in.def
	sed -e '/LIBRARY libsndfile-1.dll/d' in.def > in-mod.def
	$MSLIB /name:libsndfile-1.dll /out:sndfile.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
fi

#ffmpeg
FFmpegPrefix=lib${Prefix}
FFmpegSuffix=-lgpl
if [ "${Prefix}" = "" ]; then
	FFmpegPrefix=""
fi
if [ ! -f "$BinDir/avcodec${FFmpegSuffix}-52.dll" ]; then 
	unpack_bzip2_and_move "ffmpeg.tar.bz2" "$PKG_DIR_FFMPEG"
	mkdir_and_move "$IntDir/ffmpeg"
	
	#LGPL-compatible version
	CFLAGS=""
	CPPFLAGS="-DETIMEDOUT=10060 -D_TIMESPEC_DEFINED -D_WIN32_WINNT=0x0600 -DCOBJMACROS"
	LDFLAGS=""
	$PKG_DIR/configure --cc="$gcc" --ld="$gcc" --arch=x86 --target-os=mingw32 --enable-cross-compile --extra-ldflags="$LibFlags -Wl,--kill-at -Wl,--exclude-libs=libintl.a -Wl,--add-stdcall-alias -Wl,--no-whole-archive" --extra-cflags="$IncludeFlags -fno-lto" --enable-runtime-cpudetect --enable-yasm --enable-sse --enable-ssse3 --enable-amd3dnow --enable-mmx2 --enable-mmx --enable-avfilter --enable-avisynth --enable-memalign-hack --enable-ffmpeg --enable-ffplay --disable-ffserver --disable-debug --disable-static --enable-shared --prefix=$InstallDir --bindir=$BinDir --libdir=$LibDir --shlibdir=$BinDir --incdir=$IncludeDir 
	change_key "." "config.mak" "BUILDSUF" "${FFmpegSuffix}"
	#Adds $(SLIBPREF) to lib names when linking
	change_key "." "common.mak" "FFEXTRALIBS\ \\:" "\$\(addprefix\ -l\$\(SLIBPREF\),\$\(addsuffix\ \$\(BUILDSUF\),\$\(FFLIBS\)\)\)\ \$\(EXTRALIBS\)"
	#For some reason, FFmpeg is not enabling this even when requested
	sed \
		-e "s/#define HAVE_MMX2 0/#define HAVE_MMX2 1/g" \
		-e "s/#define HAVE_SSSE3 0/#define HAVE_SSSE3 1/g" \
		config.h > config.h.tmp && mv config.h.tmp config.h

	make ${MAKE_PARALLEL_FLAGS}
	make install
	
	#If it built successfully, then the .lib and .dll files are all in the bin/ folder with 
	#sym links. We want to take out the sym links and keep just the .lib and .dll files we need 
	#for development and execution.
	cd "$BinDir" && move_files_to_dir "av*${FFmpegSuffix}.lib" "$LibDir"
	cd "$BinDir" && move_files_to_dir "swscale*${FFmpegSuffix}.lib" "$LibDir"
	cd "$BinDir" && remove_files_from_dir "avcore${FFmpegSuffix}-*.*.*.dll avcore${FFmpegSuffix}.dll avcodec${FFmpegSuffix}-*.*.*.dll avcodec${FFmpegSuffix}.dll avdevice${FFmpegSuffix}-*.*.*.dll avdevice${FFmpegSuffix}.dll avfilter${FFmpegSuffix}-*.*.*.dll avfilter${FFmpegSuffix}.dll avformat${FFmpegSuffix}-*.*.*.dll avformat${FFmpegSuffix}.dll avutil${FFmpegSuffix}-*.*.*.dll avutil${FFmpegSuffix}.dll swscale${FFmpegSuffix}-*.*.*.dll swscale${FFmpegSuffix}.dll"
	cd "$BinDir" && remove_files_from_dir "avcore-*.lib avcodec-*.lib avdevice-*.lib avfilter-*.lib avformat-*.lib avutil-*.lib  swscale-*.lib"
	
	reset_flags
	
	cd "$IntDir/ffmpeg"
	
	cp -p "$BinDir/ffmpeg.exe" "$BinDir/ffmpeg${FFmpegSuffix}.exe"
	cp -p "$BinDir/ffplay.exe" "$BinDir/ffplay${FFmpegSuffix}.exe"
	cp -p "$BinDir/ffprobe.exe" "$BinDir/ffprobe${FFmpegSuffix}.exe"
	
	#Copy some other dlls for testing
	copy_files_to_dir "$BinDir/*.dll" "."
fi



#################
# GPL Libraries #
#################



#libnice
if [ ! -f "$BinDir/libnice-0.dll" ]; then 
	unpack_gzip_and_move "libnice.tar.gz" "$PKG_DIR_LIBNICE"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/bind.c-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/rand.c-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/agent.c-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/address.c.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/address.h-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/pseudotcp.c-win32.patch"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libnice/interfaces.c-win32.patch"
	mkdir_and_move "$IntDir/libnice"

	CFLAGS="-D_SSIZE_T_ -D_SSIZE_T_DEFINED -I$PKG_DIR -I$PKG_DIR/stun -D_WIN32_WINNT=0x0501 -DUSE_GETADDRINFO -DHAVE_GETNAMEINFO -DHAVE_GETSOCKOPT -DHAVE_INET_NTOP -DHAVE_INET_PTON"
	LDFLAGS="$LDFLAGS -lwsock32 -lws2_32 -liphlpapi -no-undefined -mno-cygwin -fno-common -fno-strict-aliasing -Wl,--exclude-libs=libintl.a -Wl,--add-stdcall-alias"
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd "nice/.libs/"
	
	$MSLIB /name:lib${Prefix}nice-0.dll /out:nice.lib /machine:$MSLibMachine /def:lib${Prefix}nice-0.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
	
	update_library_names_windows "lib${Prefix}nice.dll.a" "libnice.la"
fi

#libid3tag
if [ ! -f "$BinDir/libid3tag-0.dll" ]; then 
	unpack_gzip_and_move "libid3tag.tar.gz" "$PKG_DIR_LIBID3TAG"
	mkdir_and_move "$IntDir/libid3tag"

	#Configure isn't adding -shared to the libs so it thinks it's an exe and looks for WinMain@16 (int main() { })
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} "libid3tag_la_LIBADD=-no-undefined -shared"
	make install

	reset_flags
	
	cd .libs/
	pexports "libid3tag-0.dll" > libid3tag.def
	$MSLIB /name:libid3tag-0.dll /out:id3tag.lib /machine:$MSLibMachine /def:libid3tag.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
fi

#libmad
if [ ! -f "$BinDir/libmad-0.dll" ]; then 
	unpack_gzip_and_move "libmad.tar.gz" "$PKG_DIR_LIBMAD"
	mkdir_and_move "$IntDir/libmad"

	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} "libmad_la_LIBADD=-no-undefined -shared"
	make install
	
	cd .libs/
	pexports "libmad-0.dll" > libmad.def
	$MSLIB /name:libmad-0.dll /out:mad.lib /machine:$MSLibMachine /def:libmad.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
fi

#xvid
XvidPrefix=lib${Prefix}
if [ "${Prefix}" = "" ]; then
	XvidPrefix=""
fi
if [ ! -f "$BinDir/${XvidPrefix}xvidcore.dll" ]; then
	echo "$PKG_DIR_XVIDCORE"
	unpack_gzip_and_move "xvidcore.tar.gz" "$PKG_DIR_XVIDCORE"
	mkdir_and_move "$IntDir/xvidcore"

	cd $PKG_DIR/build/generic/
	./configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	change_key "." "platform.inc" "STATIC_LIB" "${XvidPrefix}xvidcore\.\$\(STATIC_EXTENSION\)"
	change_key "." "platform.inc" "SHARED_LIB" "${XvidPrefix}xvidcore\.\$\(SHARED_EXTENSION\)"
	change_key "." "platform.inc" "PRE_SHARED_LIB" "${XvidPrefix}xvidcore\.\$\(SHARED_EXTENSION\)"
	make ${MAKE_PARALLEL_FLAGS} && make install

	mv "$LibDir/${XvidPrefix}xvidcore.dll" "$BinDir"
	mv "$PKG_DIR/build/generic/=build/${XvidPrefix}xvidcore.dll.a" "$LibDir/libxvidcore.dll.a"

	$MSLIB /name:${XvidPrefix}xvidcore.dll /out:xvidcore.lib /machine:$MSLibMachine /def:libxvidcore.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	rm -f "$LibDir/${XvidPrefix}xvidcore.a"
fi

#wavpack
if [ ! -f "$BinDir/libwavpack-1.dll" ]; then 
	unpack_bzip2_and_move "wavpack.tar.bz2" "$PKG_DIR_WAVPACK"
	mkdir_and_move "$IntDir/wavpack"
	
	cp -p -f "$LIBRARIES_PATCH_DIR/wavpack/Makefile.in" "$PKG_DIR"
	
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd src/.libs	
	$MSLIB /name:libwavpack-1.dll /out:wavpack.lib /machine:$MSLibMachine /def:libwavpack-1.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#a52dec
if [ ! -f "$BinDir/liba52-0.dll" ]; then 
	unpack_gzip_and_move "a52.tar.gz" "$PKG_DIR_A52DEC"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/liba52/liba52-fixed.diff"
	mkdir_and_move "$IntDir/a52dec"
	
	lt_cv_deplibs_check_method=pass_all $PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	#This will error out
	make
	#Make the shared library ourselves
	cd liba52/
	$gcc -Wl,--exclude-libs=libglib-2.0.dll.a -Wl,--out-implib=liba52.dll.a -shared -lm -o liba52-0.dll bitstream.o imdct.o bit_allocate.o parse.o downmix.o
	pexports "liba52-0.dll" > .libs/liba52.exp
	cp -p .libs/liba52.exp .libs/liba52-0.dll-def
	cp -p liba52-0.dll .libs/liba52-0.dll
	cp -p liba52.dll.a .libs/liba52.dll.a
	echo -en "# Generated by ossbuild - GNU libtool 1.5.22 (1.1220.2.365 2005/12/18 22:14:06)\n" > liba52.la
	echo -en "dlname='liba52-0.dll'\n" >> liba52.la
	echo -en "library_names='liba52.dll.a'\n" >> liba52.la
	echo -en "old_library=''\n" >> liba52.la
	echo -en "inherited_linker_flags=''\n" >> liba52.la
	echo -en "dependency_libs=''\n" >> liba52.la
	echo -en "weak_library_names=''\n" >> liba52.la
	echo -en "current=0\n" >> liba52.la
	echo -en "age=0\n" >> liba52.la
	echo -en "revision=0\n" >> liba52.la
	echo -en "installed=no\n" >> liba52.la
	echo -en "shouldnotlink=no\n" >> liba52.la
	echo -en "dlopen=''\n" >> liba52.la
	echo -en "dlpreopen=''\n" >> liba52.la
	echo -en "libdir='$LibDir'\n" >> liba52.la
	
	cd .libs/
	echo -en "# Generated by ossbuild - GNU libtool 1.5.22 (1.1220.2.365 2005/12/18 22:14:06)\n" > liba52.lai
	echo -en "dlname='../bin/liba52-0.dll'\n" >> liba52.lai
	echo -en "library_names='liba52.dll.a'\n" >> liba52.lai
	echo -en "old_library=''\n" >> liba52.lai
	echo -en "inherited_linker_flags=''\n" >> liba52.lai
	echo -en "dependency_libs=' -L$LibDir'\n" >> liba52.lai
	echo -en "weak_library_names=''\n" >> liba52.lai
	echo -en "current=0\n" >> liba52.lai
	echo -en "age=0\n" >> liba52.lai
	echo -en "revision=0\n" >> liba52.lai
	echo -en "installed=yes\n" >> liba52.lai
	echo -en "shouldnotlink=no\n" >> liba52.lai
	echo -en "dlopen=''\n" >> liba52.lai
	echo -en "dlpreopen=''\n" >> liba52.lai
	echo -en "libdir='$LibDir'\n" >> liba52.lai
	cd ../
	cd ../
	
	make
	make install
	
	cp -p liba52/.libs/liba52-0.dll "$BinDir/"
	cp -p src/.libs/a52dec.exe "$BinDir/"
	$MSLIB /name:liba52-0.dll /out:a52.lib /machine:$MSLibMachine /def:liba52/.libs/liba52.exp
	move_files_to_dir "*.lib" "$LibDir/"
fi

#mpeg2
if [ ! -f "$BinDir/libmpeg2-0.dll" ]; then 
	unpack_gzip_and_move "libmpeg2.tar.gz" "$PKG_DIR_LIBMPEG2"
	mkdir_and_move "$IntDir/libmpeg2"
	
	CFLAGS="-fno-loop-block -fno-loop-strip-mine -fno-loop-interchange -fno-tree-loop-distribution -fno-tree-loop-im"
	$PKG_DIR/configure --disable-directx --disable-sdl --disable-static --enable-shared --disable-debug --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	pexports "$BinDir/libmpeg2-0.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libmpeg2-0.dll/d' in.def > in-mod.def
	$MSLIB /name:libmpeg2-0.dll /out:mpeg2.lib /machine:$MSLibMachine /def:in-mod.def
	
	pexports "$BinDir/libmpeg2convert-0.dll" | sed "s/^_//" > in-convert.def
	sed '/LIBRARY libmpeg2convert-0.dll/d' in-convert.def > in-convert-mod.def
	$MSLIB /name:libmpeg2convert-0.dll /out:mpeg2convert.lib /machine:$MSLibMachine /def:in-convert-mod.def
	
	move_files_to_dir "*.exp *.lib" "$LibDir/"

	reset_flags
fi

#libdca
if [ ! -f "$BinDir/libdca-0.dll" ]; then 
	unpack_bzip2_and_move "libdca.tar.bz2" "$PKG_DIR_LIBDCA"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libdca/libdca-llvm-gcc.patch"
	mkdir_and_move "$IntDir/libdca"
	
	$PKG_DIR/configure --enable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir
	make ${MAKE_PARALLEL_FLAGS} && make install

	rm -f "$LibDir/libdca.a"
	rm -f "$LibDir/libdts.a"
	cp -p "$LibDir/libdca.dll.a" "$LibDir/libdts.dll.a"

	pexports "$BinDir/libdca-0.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libdca-0.dll/d' in.def > in-mod.def
	$MSLIB /name:libdca-0.dll /out:dca.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
fi

#faac
if [ ! -f "$BinDir/libfaac-0.dll" ]; then 
	unpack_bzip2_and_move "faac.tar.bz2" "$PKG_DIR_FAAC"
	mkdir_and_move "$IntDir/faac"
	 
	$PKG_DIR/configure --without-mp4v2 --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir LDFLAGS="$LDFLAGS -no-undefined" 
	make ${MAKE_PARALLEL_FLAGS} && make install

	cd $PKG_DIR/libfaac
	pexports "$BinDir/libfaac-0.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libfaac-0.dll/d' in.def > in-mod.def
	$MSLIB /name:libfaac-0.dll /out:faac.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"

	reset_flags
fi

#faad
if [ ! -f "$BinDir/libfaad-2.dll" ]; then 
	unpack_bzip2_and_move "faad2.tar.bz2" "$PKG_DIR_FAAD2"
	mkdir_and_move "$IntDir/faad2"
	 
	cp "$LIBRARIES_PATCH_DIR/faad2/Makefile.in" .
	
	$PKG_DIR/configure --without-mp4v2 --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir LDFLAGS="$LDFLAGS -no-undefined" 
	make ${MAKE_PARALLEL_FLAGS} && make install
	
	cd $PKG_DIR/libfaad
	pexports "$BinDir/libfaad-2.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libfaad-2.dll/d' in.def > in-mod.def
	$MSLIB /name:libfaad-2.dll /out:faad2.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"

	reset_flags
fi

#libdl
#if [ ! -f "$BinDir/libdl.dll" ]; then 
#	unpack_bzip2_and_move "dlfcn.tar.bz2" "$PKG_DIR_DLFCN"
#	mkdir_and_move "$IntDir/libdl" 
#	 
#	cd "$PKG_DIR"
#	./configure --disable-static --enable-shared --prefix=$InstallDir --libdir=$LibDir --incdir=$IncludeDir
#
#	make && make install
#	mv "$LibDir/libdl.lib" "$LibDir/dl.lib"
#fi

#dvdread
if [ ! -f "$BinDir/libdvdread-4.dll" ]; then 
	unpack_bzip2_and_move "libdvdread.tar.bz2" "$PKG_DIR_LIBDVDREAD"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/dvdread/libdvdread-win32.patch"
	mkdir_and_move "$IntDir/libdvdread"
	 
	ac_cv_header_dlfcn_h=no sh $PKG_DIR/autogen.sh --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir LDFLAGS="$LDFLAGS"
	make ${MAKE_PARALLEL_FLAGS} && make install
	cp -f "$LIBRARIES_PATCH_DIR/dvdread/dvd_reader.h" "$IncludeDir/dvdread"/ 

	cd src/.libs
	$MSLIB /name:libdvdread-4.dll /out:dvdread.lib /machine:$MSLibMachine /def:libdvdread-4.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#dvdnav
if [ ! -f "$BinDir/libdvdnav-4.dll" ]; then 
	unpack_bzip2_and_move "libdvdnav.tar.bz2" "$PKG_DIR_LIBDVDNAV"
	patch -p0 -u -N -i "$LIBRARIES_PATCH_DIR/libdvdnav/libdvdnav.patch"
	mkdir_and_move "$IntDir/libdvdnav"
	 
	sh $PKG_DIR/autogen.sh --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir LDFLAGS="$LDFLAGS -ldvdread"
	make ${MAKE_PARALLEL_FLAGS} && make install

	cd src/.libs
	
	$MSLIB /name:libdvdnav-4.dll /out:dvdnav.lib /machine:$MSLibMachine /def:lib${Prefix}dvdnav-4.dll.def
	$MSLIB /name:libdvdnavmini-4.dll /out:dvdnavmini.lib /machine:$MSLibMachine /def:lib${Prefix}dvdnavmini-4.dll.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#dvdcss
if [ ! -f "$BinDir/libdvdcss-2.dll" ]; then 
	unpack_bzip2_and_move "libdvdcss.tar.bz2" "$PKG_DIR_LIBDVDCSS"
	mkdir_and_move "$IntDir/libdvdcss"
	 
	$PKG_DIR/configure --disable-static --enable-shared --host=$Host --build=$Build --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir 
	make ${MAKE_PARALLEL_FLAGS} && make install

	cd src/.libs
	pexports "$BinDir/libdvdcss-2.dll" | sed "s/^_//" > in.def
	sed '/LIBRARY libdvdcss-2.dll/d' in.def > in-mod.def
	$MSLIB /name:libdvdcss-2.dll /out:dvdcss.lib /machine:$MSLibMachine /def:in-mod.def
	move_files_to_dir "*.exp *.lib" "$LibDir/"
	
	reset_flags
fi

#ffmpeg GPL
if [ ! -f "$BinDir/avcodec-gpl-52.dll" ]; then 
	unpack_bzip2_and_move "ffmpeg.tar.bz2" "$PKG_DIR_FFMPEG"
	mkdir_and_move "$IntDir/ffmpeg-gpl"

	#GPL-compatible version
	CFLAGS=""
	CPPFLAGS="-DETIMEDOUT=10060 -D_TIMESPEC_DEFINED -D_WIN32_WINNT=0x0600 -DCOBJMACROS"
	LDFLAGS=""
	$PKG_DIR/configure --cc="$gcc" --ld="$gcc" --arch=x86 --target-os=mingw32 --enable-cross-compile --extra-ldflags="$LibFlags -Wl,--kill-at -Wl,--exclude-libs=libintl.a -Wl,--add-stdcall-alias -Wl,--no-whole-archive" --extra-cflags="$IncludeFlags -fno-lto" --enable-runtime-cpudetect --enable-yasm --enable-sse --enable-ssse3 --enable-amd3dnow --enable-mmx2 --enable-mmx --enable-avfilter --enable-avisynth --enable-memalign-hack --enable-ffmpeg --enable-ffplay --disable-ffserver --disable-debug --disable-static --enable-shared --enable-gpl --prefix=$InstallDir --bindir=$BinDir --libdir=$LibDir --shlibdir=$BinDir --incdir=$IncludeDir
	change_key "." "config.mak" "BUILDSUF" "-gpl"
	#Adds $(SLIBPREF) to lib names when linking
	change_key "." "common.mak" "FFEXTRALIBS\ \\:" "\$\(addprefix\ -l\$\(SLIBPREF\),\$\(addsuffix\ \$\(BUILDSUF\),\$\(FFLIBS\)\)\)\ \$\(EXTRALIBS\)"
	#For some reason, FFmpeg is not enabling this even when requested
	sed -e "s/#define HAVE_MMX2 0/#define HAVE_MMX2 1/g" -e "s/#define HAVE_SSSE3 0/#define HAVE_SSSE3 1/g" config.h > config.h.tmp
	mv config.h.tmp config.h
	
	make ${MAKE_PARALLEL_FLAGS}

	reset_flags 
	
	#Create .dll.a versions of the libs
	dlltool -U --dllname avcore-gpl-50.dll -d "libavcore/avcore-gpl-0.def" -l libavcore-gpl.dll.a
	dlltool -U --dllname avutil-gpl-50.dll -d "libavutil/avutil-gpl-50.def" -l libavutil-gpl.dll.a
	dlltool -U --dllname avcodec-gpl-52.dll -d "libavcodec/avcodec-gpl-52.def" -l libavcodec-gpl.dll.a
	dlltool -U --dllname avdevice-gpl-52.dll -d "libavdevice/avdevice-gpl-52.def" -l libavdevice-gpl.dll.a
	dlltool -U --dllname avfilter-gpl-1.dll -d "libavfilter/avfilter-gpl-1.def" -l libavfilter-gpl.dll.a
	dlltool -U --dllname avformat-gpl-52.dll -d "libavformat/avformat-gpl-52.def" -l libavformat-gpl.dll.a
	dlltool -U --dllname swscale-gpl-0.dll -d "libswscale/swscale-gpl-0.def" -l libswscale-gpl.dll.a
	
	move_files_to_dir "*.dll.a" "$LibDir/"
	
	cp -p "ffmpeg.exe" "$BinDir/ffmpeg-gpl.exe"
	cp -p "ffplay.exe" "$BinDir/ffplay-gpl.exe"
	cp -p "ffprobe.exe" "$BinDir/ffprobe-gpl.exe"
	
	cp -p "libavcore/avcore-gpl-0.dll" "."
	cp -p "libavcore/avcore-gpl-0.dll" "$BinDir/"
	cp -p "libavcore/avcore-gpl-0.lib" "$LibDir/avcore-gpl.lib"
	
	cp -p "libavutil/avutil-gpl-50.dll" "."
	cp -p "libavutil/avutil-gpl-50.dll" "$BinDir/"
	cp -p "libavutil/avutil-gpl-50.lib" "$LibDir/avutil-gpl.lib"
	
	cp -p "libavcodec/avcodec-gpl-52.dll" "."
	cp -p "libavcodec/avcodec-gpl-52.dll" "$BinDir/"
	cp -p "libavcodec/avcodec-gpl-52.lib" "$LibDir/avcodec-gpl.lib"
	
	cp -p "libavdevice/avdevice-gpl-52.dll" "."
	cp -p "libavdevice/avdevice-gpl-52.dll" "$BinDir/"
	cp -p "libavdevice/avdevice-gpl-52.lib" "$LibDir/avdevice-gpl.lib"
	
	cp -p "libavfilter/avfilter-gpl-1.dll" "."
	cp -p "libavfilter/avfilter-gpl-1.dll" "$BinDir/"
	cp -p "libavfilter/avfilter-gpl-1.lib" "$LibDir/avfilter-gpl.lib"
	
	cp -p "libavformat/avformat-gpl-52.dll" "."
	cp -p "libavformat/avformat-gpl-52.dll" "$BinDir/"
	cp -p "libavformat/avformat-gpl-52.lib" "$LibDir/avformat-gpl.lib"
	
	cp -p "libswscale/swscale-gpl-0.dll" "."
	cp -p "libswscale/swscale-gpl-0.dll" "$BinDir/"
	cp -p "libswscale/swscale-gpl-0.lib" "$LibDir/swscale-gpl.lib"
	
	#Copy some other dlls for testing
	copy_files_to_dir "$BinDir/*.dll" "."
fi

#Cleanup
$rm -rf "$LibDir/gio/"
$rm -rf "$LibDir/glib-2.0/"
$rm -rf "$LibDir/mozilla/"
$rm -rf "$LibDir/pango/"
$rm -rf "$LibDir/orc/"
$rm -rf "$LibDir/gtk-3.0/"

#Fix GTK paths
cd "$LibDir/gdk-pixbuf-2.0/2.10.0" && sed -e "s:# LoaderDir = .*:# LoaderDir = ./loaders:g" -e "s:.*libpixbufloader-svg.dll:\"./loaders/libpixbufloader-svg.dll:g" loaders.cache > loaders.cache.temp && mv loaders.cache.temp loaders.cache
cd "$EtcDir/gtk-2.0/" && grep -v -E 'Automatically generated|Created by|ModulesPath =' < gtk.immodules > gtk.immodules.temp && mv gtk.immodules.temp gtk.immodules

#Fix Pango paths
cd "$EtcDir/pango/" && grep -v -E 'Automatically generated|Created by|ModulesPath =' < pango.modules > pango.modules.temp && mv pango.modules.temp pango.modules

#Strip executables/shared libs
cd "$BinDir/"
$strip *.dll
$strip *.exe

reset_flags

#Make sure the shared directory has all our updates
create_shared

#Cleanup CRT
crt_shutdown

#Call common shutdown routines
common_shutdown

