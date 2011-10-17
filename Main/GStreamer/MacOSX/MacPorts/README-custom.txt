Using GStreamer-Macbuilds scripts to generate customized gstreamer binaries that can be bundled inside an application

1) Build step
Use the build.py script to generate gstreamer binaries for the desired cpu architecture (i386 for 32 bits, x86_64 for 64 bits) and target platform (currently Leopard and Snow Leopard)

1.1) Sample build commands:

32 bits for Snow Leopard:

> ./build.py --arch i386 --target 10.6 --dir /System/Library/Frameworks/GStreamer.framework/Versions/0.10.35-i386  --macports 2.0.3 --lrepo ./repository -noregscan

64 bits for Snow Leopard:

> ./build.py --arch x86_64 --target 10.6 --dir /System/Library/Frameworks/GStreamer.framework/Versions/0.10.35-x86_64  --macports 2.0.3 --lrepo ./repository -noregscan

2) Cleaning-up step
The folder with the required dylib and so files is <prefix>/lib. Inside lib you can delete all subfolders except gstreamer-0.10, which contains the plugins. All the files that are not either dylib and so can be deleted. Inside the plugins folder, all the files but the so can be deleted.
When copying these files to another location for the post-processing steps, keep in mind that using the cp command from the terminal replaces the symbolic links by the files they point to. This doesn't happen when copying with Finder.

3) Post-processing steps

3.0) Make sure that you have write access to all the compiled files (it might not be the case if sudo was used with the build.py tool)
> sudo chown -R <user name> <lib folder>
> chmod -R +w <lib folder>

3.1) If also needed, eliminate symbolic links using the removesymlinks.py tool:
> ./removesymlinks.py --dir <lib folder>

3.2) Remove dead dependencies (run twice to remove dependencies that were used only by dead dependencies).
> ./removedeaddeps.py --dir ../0.10.35/i386/

3.3) Replace absolute paths with relative paths using the replacepath.py tool.
For base libs:

> ./replacepath.py --old /opt/local/lib/ --new @loader_path/ --dir <lib folder>

For plugins (note that here we use the relative path @loader_path/../ instead of @loader_path/):

> ./replacepath.py --old /opt/local/lib/ --new @loader_path/../ --dir <lib folder>/gstreamer-0.10

Note that the /opt/local/lib/ path should be replaced by the install location specified in the build step.

4) Strip debug symbols:

4.1) OSX: 
strip -x *.dylib
strip -x *.so

4.2) MinGW:
strip -s *.dll
