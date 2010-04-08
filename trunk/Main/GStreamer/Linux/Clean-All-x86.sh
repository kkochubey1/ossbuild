#!/bin/sh

###############################################################################
#                                                                             #
#                             Linux x86 Clean                                 #
#                                                                             #
# Cleans out any gstreamer elements, plugins, etc.                            #
#                                                                             #
###############################################################################

TOP=$(dirname $0)/../../..

#Call common startup routines to load a bunch of variables we'll need
. $TOP/Shared/Scripts/Common.sh
common_startup "Linux" "x86" "Release"

#Load in our own common variables
. $CURR_DIR/Common.sh




echo "Removing intermediate directory: $BUILD_DIR"
rm -rf "$BUILD_DIR"

echo "Cleaning precompiled shared binaries directory (preserving VCS folders): $SharedOutDir"
cd "$SharedBinDir/"
find ./*gst*-${GstApiVersion}* -type f -perm /200 -exec rm -rf {} \;
find ./*gst*-${GstApiVersion}* -type l -exec rm -f {} \;

cd "$SharedEtcDir/"
find ./gconf/schemas/*gst*-${GstApiVersion}* -type f -perm /200 -exec rm -rf {} \;

cd "$SharedIncludeDir/"
find ./gstreamer-${GstApiVersion}/* -type f -perm /200 -exec rm -rf {} \;

cd "$SharedLibDir/"
find ./*libgst*-${GstApiVersion}*.la -type f -perm /200 -exec rm -rf {} \;
find ./pkgconfig/*gst*-${GstApiVersion}*.pc -type f -perm /200 -exec rm -rf {} \;
find ./pkgconfig/*farsight*-${GstApiVersion}*.pc -type f -perm /200 -exec rm -rf {} \;
find ./python2.6/site-packages/*farsight* -type f -perm /200 -exec rm -rf {} \;
find ./python2.6/site-packages/*gst* -type f -perm /200 -exec rm -rf {} \;
find ./python2.6/site-packages/gst-${GstApiVersion}/* -type f -perm /200 -exec rm -rf {} \;
find ./mono/* -type l -exec rm -f {} \;
find ./mono/* -type f -perm /200 -exec rm -rf {} \;
find ./gstreamer-${GstApiVersion}/* -type l -exec rm -f {} \;
find ./gstreamer-${GstApiVersion}/* -type f -perm /200 -exec rm -rf {} \;
find ./farsight*-${Farsight2ApiVersion}/* -type f -perm /200 -exec rm -rf {} \;

cd "$SharedShareDir/"
find ./aclocal/*gst*-${GstApiVersion}*.m4 -type f -perm /200 -exec rm -rf {} \;
find ./gapi/*gstreamer*.xml -type f -perm /200 -exec rm -rf {} \;
find ./gst-python/* -type f -perm /200 -exec rm -rf {} \;
find ./gstreamer-${GstApiVersion}/* -type f -perm /200 -exec rm -rf {} \;
find ./locale/* -name "*gst*-${GstApiVersion}*.mo" -type f -exec rm -rf {} \;

cd "$SharedTemplateLibDir/"
find ./*libgst*-${GstApiVersion}*.la.in -type f -perm /200 -exec rm -rf {} \;

cd "$SharedTemplatePkgConfigDir/"
find ./*gst*-${GstApiVersion}*.pc.in -type f -perm /200 -exec rm -rf {} \;
find ./*farsight*-${GstApiVersion}*.pc.in -type f -perm /200 -exec rm -rf {} \;




#Call common shutdown routines
common_shutdown

