#!/usr/bin/env python
# This script compiles gstreamer on OSX using MacPorts.
# Authors: Andres Colubri (http://interfaze.info/)
#          David Liu (http://archive.itee.uq.edu.au/~davel/)
# Part of the OSSBuild project:
# http://code.google.com/p/ossbuild/

# NOTES:
# Currently there is an issue when compiling python27 using 10.5 as target. Enablig X11 support
# has python27 as dependency.
# https://trac.macports.org/ticket/27916

import sys
import os
import subprocess

def print_help():
    print 'This script compiles gstreamer on OSX using MacPorts. It accepts the following arguments:'
    print '--arch architecture. Expected values: i386, x64. Default: x64'
    print '--target target OS version. Expected values: 10.5, 10.6. Default: 10.6'
    print '--dir desired base location of the generated files. Default: /opt/local'
    print '--macports MacPorts version. Default: 1.9.2'
    print 'Note that if the base folder is set to be a folder that requires root access to be written to,'
    print 'then this script should be run with sudo.'
    print 'Also note that, by default, doesn`t build gstreamer with X11 support. Add the -X11 switch'
    print 'to override this setting.'
    print 'For example, the following line would build gstreamer for 32 bits using MacPorts 1.9.2,'
    print 'targeted at OSX Leopard, put the files in /opt/local, and enable X11 support:'
    print 'build.py --arch i386 --target 10.5 --folder /opt/local --macports 1.9.2 -X11'

def print_error(error):
    print error + '. Get some help by typing:'
    print 'build.py --help'     
    
def run_cmd(cmd, title):
    print title + '...'
    proc = subprocess.Popen(cmd, shell=True)
    sts = os.waitpid(proc.pid, 0)[1]    
    if sts:
        sys.exit('ERROR AT ' + title)
    else:
        print 'DONE.'
    
def download_macports(macports_str, macports_dir):
    if os.path.exists(macports_dir):
        os.chdir(macports_dir)
        run_cmd('make clean', 'CLEANING-UP MACPORTS')
    else:
        run_cmd('curl -O http://distfiles.macports.org/MacPorts/' + macports_str + '.tar.bz2', 'DOWNLOADING MACPORTS')
        run_cmd('tar xvfz MacPorts-1.9.2.tar.bz2', 'UNCOMPRESSING MACPORTS')
        os.chdir(macports_dir)
        
def setup_macports(cpu_arch, install_dir):
    run_cmd('./configure --build=' + cpu_arch + ' --prefix=' + install_dir, 'CONFIGURING MACPORTS')
    run_cmd('make', 'COMPILING MACPORTS')
    run_cmd('make install', 'INSTALLING MACPORTS')
    run_cmd(install_dir + '/bin/port -v selfupdate', 'UPDATING MACPORTS')

def fix_ldflags(install_dir, target_os):
    config_file = install_dir + '/share/macports/Tcl/port1.0/portconfigure.tcl'

    orig_str = 'default configure.ldflags   {-L${prefix}/lib}'

    if target_os < 6:
        # When targeting versions of OSX older than Snow Leopard, some linker flags that ensure 
        # compatibility with previous library loading mechanisms are needed. Discussed here:
        # http://lists.apple.com/archives/xcode-users/2009/Oct/msg00514.html
        osx_str = '10.' + str(target_os)        
        new_str = 'default configure.ldflags   {"-L${prefix}/lib -Xlinker -headerpad_max_install_names -mmacosx-version-min=' + osx_str + ' -no_compact_linkedit"}'    
    else:
        # This additional linker flag allows to later  change the absolute dependency paths embedded 
        # in the .dylib and .so files by relative paths.
        new_str = 'default configure.ldflags   {"-L${prefix}/lib -Xlinker -headerpad_max_install_names"}'

    run_cmd("sed -i -e 's|" + orig_str + "|" + new_str + "|' " + config_file, 'FIXING portconfigure.tcl')

def fix_conf(install_dir, target_os):
    if target_os < 6:
        config_file = install_dir + '/etc/macports/macports.conf'
        run_cmd('echo macosx_deployment_target 10.' + str(target_os) + ' >> ' + config_file, 'FIXING macports.conf')

def delete_files():
    files = ['/Library/LaunchAgents/org.freedesktop.dbus-session.plist',
             '/Library/LaunchDaemons/org.freedesktop.dbus-system.plist']

    print 'REMOVING LEFTOVER FILES...'
    for fname in files:
        if os.path.lexists(fname):
            os.remove(fname)
    print 'DONE.'

def build_gstreamer(install_dir, use_x11):
    port_str = install_dir + '/bin/port install '
    if use_x11:
        run_cmd(port_str + 'gstreamer', 'BUILDING gstreamer')
        run_cmd(port_str + 'gst-plugins-base', 'BUILDING gst-plugins-base')
        run_cmd(port_str + 'gst-plugins-good', 'BUILDING gst-plugins-good')
        run_cmd(port_str + 'gst-plugins-bad', 'BUILDING gst-plugins-bad')
        run_cmd(port_str + 'gst-plugins-ugly', 'BUILDING gst-plugins-ugly')
        run_cmd(port_str + 'gst-ffmpeg', 'BUILDING gst-ffmpeg')
        run_cmd(port_str + 'gst-plugins-gl', 'BUILDING gst-plugins-gl')
        run_cmd(port_str + 'gnonlin', 'BUILDING gnonlin')
    else:
        run_cmd(port_str + 'gstreamer', 'BUILDING gstreamer')
        run_cmd(port_str + 'gst-plugins-base +no_x11 +no_gnome_vfs', 'BUILDING gst-plugins-base')
        run_cmd(port_str + 'gst-plugins-good', 'BUILDING gst-plugins-good')
        run_cmd(port_str + 'gst-plugins-bad +no_x11', 'BUILDING gst-plugins-bad')
        run_cmd(port_str + 'gst-plugins-ugly', 'BUILDING gst-plugins-ugly')
        run_cmd(port_str + 'gst-ffmpeg', 'BUILDING gst-ffmpeg')
        run_cmd(port_str + 'gst-plugins-gl', 'BUILDING gst-plugins-gl')
        run_cmd(port_str + 'gnonlin', 'BUILDING gnonlin')

def main():
    SUPP_ARCH = ['i386', 'x64']
    cpu_arch = 'x64'

    SUPP_TARGET = {'10.5':5, '10.6':6}
    target_os = 6
    
    install_dir = '/opt/local'    
    macports_ver = '1.9.2'
    use_x11 = 0
    
    next_is_value = 0
    l = len(sys.argv)
    if 1 < l:
        for i in range(1, l):
            if next_is_value:
                next_is_value = 0 
                continue
            arg = sys.argv[i]
            if arg == '--arch':
                if i + 1 < l:
                    next_is_value = 1
                    cpu_arch = sys.argv[i+1]
                    if not cpu_arch in SUPP_ARCH:
                        print_error('Wrong argument value')
                        return                    
                else:
                    print_error('Missing argument value')
                    return
            elif arg == '--target':  
                if i + 1 < l:
                    next_is_value = 1
                    val = sys.argv[i+1]
                    if val in SUPP_TARGET:
                        target_os = SUPP_TARGET[val]
                    else:
                        print_error('Wrong argument value')
                        return                                        
                else:
                    print_error('Missing argument value')
                    return                
            elif arg == '--dir':
                if i + 1 < l:
                    next_is_value = 1
                    install_dir = sys.argv[i+1]
                else:
                    print_error('Missing argument value')
                    return            
            elif arg == '--macports':
                if i + 1 < l:
                    next_is_value = 1
                    macports_ver = sys.argv[i+1]
                else:
                    print_error('Missing argument value')
                    return            
            elif arg == '-X11':
                use_x11 = 1                        
            elif arg == '--help':
                print_help()
                return
            else:
                print_error('Unknown argument (' + arg + ')')
                return
    else:
        print_help()
        return
    
    macports_str = 'MacPorts-' + macports_ver
    macports_dir = './' + macports_str
        
    download_macports(macports_str, macports_dir)
    setup_macports(cpu_arch, install_dir)
    fix_ldflags(install_dir, target_os)
    fix_conf(install_dir, target_os)
    delete_files()
    build_gstreamer(install_dir, use_x11)    
main()