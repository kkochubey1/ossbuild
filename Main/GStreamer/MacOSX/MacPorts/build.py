#!/usr/bin/env python
# This script compiles gstreamer on OSX using MacPorts.
# Author: Andres Colubri (http://interfaze.info/), David Liu (http://archive.itee.uq.edu.au/~davel/)
# Part of the OSSBuild project:
# http://code.google.com/p/ossbuild/

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
    
def run_cmd(cmd):
    proc = subprocess.Popen(cmd, shell=True)
    sts = os.waitpid(proc.pid, 0)[1]    

def compile_macports(cpu_arch, install_dir):
    print 'COMPILING MACPORTS...'
    run_cmd('./configure --build=' + cpu_arch + ' --prefix=' + install_dir)
    run_cmd('make')
    run_cmd('make install')
    run_cmd(install_dir + '/bin/port -v selfupdate')

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
    if os.path.exists(macports_dir):
        os.chdir(macports_dir)
        print 'CLEANING-UP MACPORTS...'        
        run_cmd('make clean')
    else:
        print 'DOWNLOADING MACPORTS...'
        run_cmd('curl -O http://distfiles.macports.org/MacPorts/' + macports_str + '.tar.bz2')
        print 'UNCOMPRESSING MACPORTS...'
        run_cmd('tar xvfz MacPorts-1.9.2.tar.bz2')
        os.chdir(macports_dir)
    
    compile_macports(cpu_arch, install_dir)
    
main()