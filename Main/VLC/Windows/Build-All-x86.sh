#!/bin/sh

###############################################################################
#                                                                             #
#                             Linux x86 Build                                 #
#                                                                             #
# Builds all gstreamer elements, plugins, etc.                                #
#                                                                             #
###############################################################################

TOP=$(dirname $0)/../../..

#Call common startup routines to load a bunch of variables we'll need
. $TOP/Shared/Scripts/Common.sh
common_startup "Windows" "Win32" "Release" "x86" "" ""

#Select which MS CRT we want to build against (e.g. msvcr90.dll)
. $ROOT/Shared/Scripts/CRT-x86.sh
crt_startup

#Setup library versions
. $ROOT/Shared/Scripts/Version.sh

#Load in our own common variables
. $MAIN_DIR/VLC/Common.sh

#Move to intermediate directory
cd "$IntDir"

#Add MS build tools
setup_ms_build_env_path

#Ensure that whoami and hostname provide helpful values
create_whoami_windows
create_hostname_windows

#Ensure that gcc outputs 32-bit
create_wrapper_windows "/mingw/bin/windres" "--target=pe-i386"
create_wrapper_windows "/mingw/bin/dlltool" "-m i386 -f--32"
create_wrapper_windows "/mingw/bin/gcc" "-m32"
create_wrapper_windows "/mingw/bin/g++" "-m32"
create_wrapper_windows "/mingw/bin/c++" "--32"
create_wrapper_windows "/mingw/bin/cpp" "-m32"

#Download and load pkg-config dev pkg needed for bootstrapping
cd "$VlcTmpDir/"
if [ ! -f "$VlcTmpDir/pkg-config.zip" ]; then
	wget --no-check-certificate -O pkg-config.zip "http://ftp.gnome.org/pub/gnome/binaries/win32/dependencies/pkg-config_0.23-3_win32.zip"
	7za -y "-o$MinGWDir" x pkg-config.zip
	
	wget --no-check-certificate -O pkg-config-dev.zip "http://ftp.gnome.org/pub/gnome/binaries/win32/dependencies/pkg-config-dev_0.23-3_win32.zip"
	7za -y "-o$MinGWDir" x pkg-config-dev.zip
fi

unpack_gzip_and_move "vlc.tar.gz" "$PKG_DIR_VLC"
patch -p0 -u -N -i "$VlcPatchDir/winvlc.c.patch"
patch -p0 -u -N -i "$VlcPatchDir/vlc_windows_interfaces.h.patch"
./bootstrap

#We need to cycle through this twice to make 2 builds:
#one which references FFmpeg LGPL libs and another for GPL
for FFmpegSuffix in $LicenseSuffixes
do
	if [ ! -f "$VlcSharedPluginLibDir/libavcodec_plugin${FFmpegSuffix}.dll" ]; then 
		mkdir_and_move "$VlcIntDir/vlc${FFmpegSuffix}"
		
		PKG_CONFIG_PATH="$VlcIntDir/vlc${FFmpegSuffix}:$PKG_CONFIG_PATH"
		sed -e "s/-lavcodec/-lavcodec${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libavcodec.pc > libavcodec.pc
		sed -e "s/-lavdevice/-lavdevice${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libavdevice.pc > libavdevice.pc
		sed -e "s/-lavfilter/-lavfilter${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libavfilter.pc > libavfilter.pc
		sed -e "s/-lavformat/-lavformat${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libavformat.pc > libavformat.pc
		sed -e "s/-lavutil/-lavutil${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libavutil.pc > libavutil.pc
		sed -e "s/-lswscale/-lswscale${FFmpegSuffix}/g" $SharedLibDir/pkgconfig/libswscale.pc > libswscale.pc
		
		CFLAGS="$CFLAGS -DHAVE_LLDIV"
		$PKG_DIR/configure \
			$VlcConfigureDefault \
			--enable-mmx \
			--enable-sse \
			--disable-lua \
			--disable-httpd \
			--disable-libproxy \
			--enable-dshow \
			--enable-bda \
			--disable-dxva2 \
			--enable-snapshot \
			--enable-directx \
			--enable-wingdi \
			--enable-waveout \
			--disable-qt4 \
			--disable-skins2 \
			--disable-activex \
			--disable-mozilla \
			--disable-mad \
			--disable-postproc \
			--enable-vlc 
		make -j3 
		make install

		cd src/.libs/
		$MSLIB /name:libvlc.dll /out:vlc.lib /machine:$MSLibMachine /def:libvlc.dll.def
		$MSLIB /name:libvlccore.dll /out:vlccore.lib /machine:$MSLibMachine /def:libvlccore.dll.def
		move_files_to_dir "*.exp *.lib" "$LibDir/"
		cd ../../
		
		mkdir -p "$LibDir/"
		mkdir -p "$PkgConfigDir/"
		mkdir -p "$VlcPluginLibDir"
		mkdir -p "$VlcSharedPluginLibDir"
		
		cd "$BinDir/"
		
		mv vlc/vlc-cache-gen.exe .
		find ./vlc/plugins/ -type f -name "*.dll" -exec mv {} "$VlcPluginLibDir/" \;
		rm -rf vlc/
		
		cd "$VlcPluginLibDir/" && copy_files_to_dir "*" "$VlcSharedPluginLibDir/"
		
		cd "$VlcSharedPluginLibDir/"
		mv libaccess_avio_plugin.dll libaccess_avio_plugin${FFmpegSuffix}.dll
		mv libavcodec_plugin.dll libavcodec_plugin${FFmpegSuffix}.dll
		mv libavformat_plugin.dll libavformat_plugin${FFmpegSuffix}.dll
		mv libswscale_plugin.dll libswscale_plugin${FFmpegSuffix}.dll
		
		reset_flags
		reset_pkgconfig_path
	fi
done



#Make sure the shared directory has all our updates
create_shared

#Cleanup CRT
crt_shutdown

#Call common shutdown routines
common_shutdown

