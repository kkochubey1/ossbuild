Using GStreamer-Macbuilds scripts to generate customized gstreamer binaries that can be bundled inside an application

1) Build step
Use the build.py script to generate gstreamer binaries for the desired cpu architecture (i386 for 32 bits, x86_64 for 64 bits) and target platform (currently Leopard and Snow Leopard)

2) Cleaning-up step
The folder with the required dylib and so files is <prefix>/lib. Inside lib you can delete all subfolders except gstreamer-0.10, which contains the plugins. All the files that are not either dylib and so can be deleted. Inside the plugins folder, all the files but the so can be deleted.

3) Post-processing steps

3.1) If also needed, eliminate symbolic links using the removesymlinks.py tool:
> ./removesymlinks.py --dir <lib folder>

3.2) Replace absolute paths with relative paths using the replacepath.py tool.
For base libs:
> ./replacepath.py --old /opt/local/lib/ --new @loader_path/ --dir <lib folder>
For plugins (note that here we use the relative path @loader_path/../ instead of @loader_path/):
> ./replacepath.py --old /opt/local/lib/ --new @loader_path/../ --dir <lib folder>/gstreamer-0.10

