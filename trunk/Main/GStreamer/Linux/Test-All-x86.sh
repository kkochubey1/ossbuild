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
export GST_REGISTRY_FORK=no
export GST_REGISTRY_UPDATE=yes
export GST_PLUGIN_SYSTEM_PATH=
export GST_PLUGIN_SCANNER=$GstPluginLibDir/gst-plugin-scanner

export PATH=$GstPluginLibDir:$PATH

rm -f $GST_REGISTRY


#Core
mkdir_and_move "$GstIntDir/gstreamer"
make check

#Base plugins
mkdir_and_move "$GstIntDir/gst-plugins-base"
make check

#Good plugins
mkdir_and_move "$GstIntDir/gst-plugins-good"
make check

#Ugly plugins
mkdir_and_move "$GstIntDir/gst-plugins-ugly"
make check

#Bad plugins
mkdir_and_move "$GstIntDir/gst-plugins-bad"
make check

#OpenGL plugins
mkdir_and_move "$GstIntDir/gst-plugins-gl"
make check

#FFmpeg plugins (GPL and LGPL)
#for FFmpegSuffix in $LicenseSuffixes
#do
#	mkdir_and_move "$GstIntDir/gst-ffmpeg${FFmpegSuffix}"
#	make check
#done

#GStreamer GNonlin plugins
mkdir_and_move "$GstIntDir/gnonlin"
make check

#GStreamer Python bindings
mkdir_and_move "$GstIntDir/gst-python"

#Farsight2
mkdir_and_move "$GstIntDir/farsight2"

#GStreamer Farsight2 plugins
mkdir_and_move "$GstIntDir/gst-plugins-farsight"

#GStreamer Sharp (.NET) bindings
mkdir_and_move "$GstIntDir/gstreamer-sharp"




#Call common shutdown routines
common_shutdown

