#!/bin/sh

###############################################################################
#                                                                             #
#                             Linux x86 Test                                  #
#                                                                             #
# Tests gstreamer elements, plugins, etc.                                     #
#                                                                             #
###############################################################################

TOP=$(dirname $0)/../../..

#Call common startup routines to load a bunch of variables we'll need
. $TOP/Shared/Scripts/Common.sh
common_startup "Linux" "x86" "Release"

#Load in our own common variables
. $CURR_DIR/Common.sh

export GST_REGISTRY=/tmp/gst-registry.bin
export GST_PLUGIN_PATH=$GstPluginLibDir
export GST_REGISTRY_FORK=yes
export GST_REGISTRY_UPDATE=no
export GST_PLUGIN_SYSTEM_PATH=$GstPluginLibDir
export GST_PLUGIN_SCANNER=$GstPluginLibDir/gst-plugin-scanner
export GST_PLUGINS_DIR=$GST_PLUGIN_PATH

export PATH=$GstPluginLibDir:$GstPluginBinDir:$PATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GstPluginLibDir

#Prep environment by removing the gst registry and moving plugins that have issues 
#with their GPL counterpart or are language binding-specific
rm -f $GST_REGISTRY
mv $GstPluginLibDir/libgstpython.so /tmp/libgstpython.so.tmp
mv $GstPluginLibDir/libgstffmpeg-lgpl.so /tmp/libgstffmpeg-lgpl.so.tmp
mv $GstPluginLibDir/libgstffmpegscale-lgpl.so /tmp/libgstffmpegscale-lgpl.so.tmp

update_plugins_dir() {
	cd tests/check
	change_key . Makefile GST_PLUGINS_DIR $GST_PLUGIN_PATH
	cd ../../
}




#Core
mkdir_and_move "$GstIntDir/gstreamer"
make check

#Base plugins
mkdir_and_move "$GstIntDir/gst-plugins-base"
update_plugins_dir
make check

#Good plugins
mkdir_and_move "$GstIntDir/gst-plugins-good"
update_plugins_dir
make check

#Ugly plugins
mkdir_and_move "$GstIntDir/gst-plugins-ugly"
update_plugins_dir
make check

#Bad plugins
mkdir_and_move "$GstIntDir/gst-plugins-bad"
update_plugins_dir
make check

#OpenGL plugins
mkdir_and_move "$GstIntDir/gst-plugins-gl"
update_plugins_dir
make check

#FFmpeg plugins (GPL)
mkdir_and_move "$GstIntDir/gst-ffmpeg-gpl"
#make check

#GStreamer GNonlin plugins
mkdir_and_move "$GstIntDir/gnonlin"
update_plugins_dir
make check

#GStreamer Python bindings
mkdir_and_move "$GstIntDir/gst-python"

#Farsight2
mkdir_and_move "$GstIntDir/farsight2"

#GStreamer Farsight2 plugins
mkdir_and_move "$GstIntDir/gst-plugins-farsight"

#GStreamer Sharp (.NET) bindings
mkdir_and_move "$GstIntDir/gstreamer-sharp"
#make check




mv /tmp/libgstpython.so.tmp $GstPluginLibDir/libgstpython.so
mv /tmp/libgstffmpeg-lgpl.so.tmp $GstPluginLibDir/libgstffmpeg-lgpl.so
mv /tmp/libgstffmpegscale-lgpl.so.tmp $GstPluginLibDir/libgstffmpegscale-lgpl.so

#Call common shutdown routines
common_shutdown

