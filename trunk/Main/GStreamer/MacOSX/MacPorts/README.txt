GStreamer-Macbuilds
These are the steps required to compile gstreamer in OSX

1. Install macports.
and modify 
/opt/local/share/macports/Tcl/port1.0/portconfigure.tcl
in the line /opt/local/share/macports/Tcl/port1.0/portconfigure.tcl to look like this:
default configure.ldflags   {"-L${prefix}/lib -Xlinker -headerpad_max_install_names"}
Full details here:
http://www.danplanet.com/home/82-codemonkeying/88-using-py2app-with-gtk
2. Set repo to be the local portfile repository:
http://guide.macports.org/#development.local-repositories
3. Install the gstreamer ports with the following variant options:
port install gstreamer
port install gst-plugins-base +no_x11 +no_gnome_vfs
port install gst-plugins-good
port install gst-plugins-bad +no_x11 +dc1394
port install gst-plugins-ugly
port install gst-ffmpeg

Notes:
1. Things removed from official portfiles:
removed openssl in gst-plugins-base
removed gnome-keyring in gst-plugins-good
removed libsoup in gst-plugins-good
removed taglib from gst-plugins-good
removed post-activate section from gst-plugins-good
patched autogen.sh from gst-plugins-good so gtk2 option is not enabled.
removed neon from neon gst-plugins-bad
removed libglade2 from gst-plugins-bad
removed libmodplug from gst-plugins-bad until 32 bits compilation on SnowLeopard 64 bits is solved (https://svn.macports.org/ticket/24627)
removed libid3tag from gst-plugins-ugly because it doesnt't compile in 32 bits.
removed sdl and xorg-libXv from libmpeg2

2. problems with gst-ffmpeg, 32 bits compilation on Snow Leopard 64 bits:
https://trac.macports.org/ticket/24636
https://trac.macports.org/ticket/24629

Changes to make it compile. Actually only two:
#define HAVE_EBX_AVAILABLE 0 in gst-libs/ext/ffmpeg/config.h
#define HAVE_MMX 0 in gst-libs/ext/ffmpeg/libswscale/rgb2rgb_template.c

The only solution for now is to run port install gst-ffmpeg, and after the build crashes, 
change into the directory gnome/gst-ffmpeg/work/gst-ffmpeg-0.10.10, manually edit the changes 
indicated above and then sudo make; make install

3. Once the binaries are compiled it might be necessary to change the dependency information inside
the dylib files. This is needed when gstreamer will be placed in a directory different from the 
default /opt/local/lib. This is achieved with the replacepath script. Also, symlinks might need to
be removed. This can be done with removesymlinks script.
