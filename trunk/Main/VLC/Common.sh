#!/bin/sh

###############################################################################
#                                                                             #
#                            Common Settings                                  #
#                                                                             #
# Provides common variables/settings used across these scripts.               #
#                                                                             #
###############################################################################

#Load local version info.
. $MAIN_DIR/VLC/Version.sh



export VlcApiVersion=1.0
export PKG_URI="http://code.google.com/p/ossbuild/"



#Causes us to now always include the bin dir to look for libraries, even after calling reset_flags
export ORIG_LDFLAGS="$ORIG_LDFLAGS -L$BinDir -L$SharedBinDir"
reset_flags

#Setup possible license suffixes
export LicenseSuffixes='-lgpl -gpl'

#Setup VLC paths
export VlcDir=$MAIN_DIR/VLC
export VlcPackagesDir=$VlcDir/Packages
export VlcPatchDir=$VlcDir/Patches
export VlcSrcDir=$VlcDir/Source
export VlcIntDir=$IntDir

export VlcPluginLibDir=$LibDir/vlc-$VlcApiVersion
export VlcSharedPluginLibDir=$SharedLibDir/vlc-$VlcApiVersion

export VlcTmpDir=$VlcIntDir/tmp

export MSysDir=$TOOLS_DIR/msys
export MinGWDir=$MSysDir/mingw

mkdir -p $VlcTmpDir
mkdir -p $VlcPluginLibDir

#Augment paths to find our uninstalled binaries
export VLC_PATH="$BinDir:$SharedBinDir:$VlcPluginLibDir:$PATH"
export VLC_LD_LIBRARY_PATH="$BinDir:$SharedBinDir:$VlcPluginLibDir:$LD_LIBRARY_PATH"
export LD_LIBRARY_PATH=$VLC_LD_LIBRARY_PATH
export PATH=$VLC_PATH

#Setup fontconfig paths
export FONTCONFIG_FILE=$SharedEtcDir/fonts/fonts.conf
export FC_CONFIG_DIR=$SharedEtcDir/fonts

#Modify unpack dir
export LIBRARIES_UNPACK_DIR=$VlcSrcDir
export LIBRARIES_PACKAGE_DIR=$VlcPackagesDir

#Modify patch dir
export LIBRARIES_PATCH_DIR=$VlcPatchDir

#Default configure options
export VlcConfigureDefault=" --disable-debug --disable-static --enable-shared --prefix=$InstallDir --libexecdir=$BinDir --bindir=$BinDir --libdir=$LibDir --includedir=$IncludeDir "

