I) BUILD SCRIPT
build.py

II) PACKAGE PREPARATION (OPTIONAL)
make_links.py
? package manager from cmd??

III) LIBRARY CLEAN-UP (OPTIONAL)
replacepath.py
removesymlinks.py

IV) UNINSTALL

To uninstall either from compiled or installed from package you simple need to delete /System/Library/Frameworks/GStreamer.framework/ 

and the following files:
/usr/bin/gst-feedback
/usr/bin/gst-feedback-0.10
/usr/bin/gst-inspect
/usr/bin/gst-inspect-0.10
/usr/bin/gst-launch
/usr/bin/gst-launch-0.10
/usr/bin/gst-typefind
/usr/bin/gst-typefind-0.10
/usr/bin/gst-visualise-0.10
/usr/bin/gst-xmlinspect
/usr/bin/gst-xmlinspect-0.10
/usr/bin/gst-xmllaunch
/usr/bin/gst-xmllaunch-0.10

The installer doesn't mess with any path settings etc... it happens to be one of the nice side effects of MacPorts hard-coding all of the library paths into the binaries.

