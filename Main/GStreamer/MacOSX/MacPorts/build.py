#!/usr/bin/env python
# This script compiles gstreamer on OSX using MacPorts.
# Authors: Andres Colubri (http://interfaze.info/)
#          David Liu (http://archive.itee.uq.edu.au/~davel/)
# Version: 0.5 (2011-01-24)
# Part of the OSSBuild project:
# http://code.google.com/p/ossbuild/

# NOTES:
# Currently there is an issue when compiling python27 using 10.5 as target. Enablig X11 support
# has python27 as dependency.
# https://trac.macports.org/ticket/27916

import sys
import os
import subprocess

session_file = 0
session_step = 0

def print_help():
    print 'This script compiles gstreamer on OSX using MacPorts. It accepts the following arguments:'
    print '--arch architecture. Expected values: i386, x86_64. Default: x86_64'
    print '--target target OS version. Expected values: 10.5, 10.6. Default: 10.6'
    print '--dir desired base location of the generated files. Default: /opt/local'
    print '--macports MacPorts version. Default: 1.9.2'
    print '--lrepo adds a local portfile repository. By default, no local repositories are used.'
    print ' '
    print 'Notes:' 
    print '1) If the base folder is set to be a folder that requires root access to be written to,'
    print '   then this script should be run with sudo.'
    print '2) By default, it doesn`t build gstreamer with X11 support. Add the -X11 switch to override'
    print '   this setting.'
    print '3) If execution stops due to an error in some intermediate step, the next time the script'
    print '   is called it is possible to resume from the last step by adding the -resume switch.'
    print '4) The switch -noregscan can be added to disable the use of the registry scanner helper.'
    print '   In this case, gstreamer will use the old plugin scanning method that doesn`t require'
    print '   an external helper binary.'
    print ' '
    print 'Examples:' 
    print '1) The following line would build gstreamer for 32 bits using MacPorts 1.9.2, targeted'
    print '   at OSX Leopard, put the files in /opt/local, and enable X11 support:'
    print 'build.py --arch i386 --target 10.5 --dir /opt/local --macports 1.9.2 -X11'
    print '2) To try to resume execution from the last succesful step with the parameters used in'
    print '   the previous call:'
    print 'build.py -resume'

def print_error(error):
    print error + '. Get some help by typing:'
    print 'build.py --help'     
    
def run_cmd(cmd, title):
    global session_file
    global session_step

    print title + '...'
    proc = subprocess.Popen(cmd, shell=True)
    sts = os.waitpid(proc.pid, 0)[1]    
    if sts:
        session_file.close()
        sys.exit('ERROR AT ' + title)
    else:
        print 'DONE.'
    
def download_macports(macports_str, macports_dir):
    if os.path.exists(macports_dir):
        dir0 = os.getcwd()
        os.chdir(macports_dir)
        run_cmd('make clean', 'CLEANING-UP MACPORTS')
        os.chdir(dir0)
    else:
        run_cmd('curl -O http://distfiles.macports.org/MacPorts/' + macports_str + '.tar.bz2', 'DOWNLOADING MACPORTS')
        run_cmd('tar xvfz MacPorts-1.9.2.tar.bz2', 'UNCOMPRESSING MACPORTS')
        
def setup_macports(cpu_arch, macports_dir, install_dir):
    dir0 = os.getcwd()
    os.chdir(macports_dir)
    run_cmd('./configure --build=' + cpu_arch + ' --prefix=' + install_dir, 'CONFIGURING MACPORTS')
    run_cmd('make', 'COMPILING MACPORTS')	
    run_cmd('make install', 'INSTALLING MACPORTS')
    run_cmd(install_dir + '/bin/port -v selfupdate', 'UPDATING MACPORTS')
    os.chdir(dir0)

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

def fix_conf(install_dir, target_os, cpu_arch):    
    config_file = install_dir + '/etc/macports/macports.conf'
    
    if target_os < 6:        
        run_cmd('echo macosx_deployment_target 10.' + str(target_os) + ' >> ' + config_file, 'ADDING TARGET OS TO macports.conf')
        
    run_cmd('echo build_arch ' + cpu_arch + ' >> ' + config_file, 'ADDING CPU ARCH. TO macports.conf')

def add_local_repos(install_dir, local_repos):
    if len(local_repos) == 0: return

    def_repo_str = 'rsync://rsync.macports.org/release/ports/ [default]'    
    sources_fn = install_dir + '/etc/macports/sources.conf'
    portindex_str = install_dir + '/bin/portindex'
    
    ifile = open(sources_fn)
    input = ifile.readlines()
    ifile.close()
    
    index = next((i for i in xrange(len(input) - 1, -1, -1) if input[i].strip() == def_repo_str), None)
    
    n = 0
    for lrepo in local_repos:
        lrepo_path = os.path.realpath(lrepo)
        lrepo_str = 'file://' + lrepo_path
        index0 = next((i for i in xrange(len(input) - 1, -1, -1) if input[i].strip() == lrepo_str), None)        
        if index0 == None:
            input.insert(index + n, lrepo_str + '\n')
            n = n + 1
    
    ofile = open(sources_fn, 'w')
    for line in input: ofile.write(line)
    ofile.close()

    dir0 = os.getcwd()
    for lrepo in local_repos:
        lrepo_path = os.path.realpath(lrepo)
        os.chdir(lrepo_path)
        run_cmd(portindex_str, 'ADDING LOCAL REPOSITORY ' + lrepo)
    os.chdir(dir0)
    
def delete_files():
    files = ['/Library/LaunchAgents/org.freedesktop.dbus-session.plist',
             '/Library/LaunchDaemons/org.freedesktop.dbus-system.plist',
             '/Library/Ruby/Site/1.8/caca.rb',
             '/Library/Ruby/Site/1.8/universal-darwin10.0/caca.la',
             '/Library/Ruby/Site/1.8/universal-darwin10.0/caca.so']

    print 'REMOVING LEFTOVER FILES...'
    for fname in files:
        if os.path.lexists(fname):
            os.remove(fname)
    print 'DONE.'

def build_gstreamer(install_dir, cpu_arch, target_os, use_x11, use_regscanner):
    port_str = install_dir + '/bin/port install '
    
    if use_regscanner:
        regscanner = ' +regscanner'
    else:
        regscanner = ''
      
    if cpu_arch == 'x86_64':   
        if 6 <= target_os:
            apple_media = ' +apple_media'
            build_gstgl = 1
        else: 
            apple_media = ''
            build_gstgl = 0        
    else:
        apple_media = ''
        build_gstgl = 0
        
    if use_x11:
        run_cmd(port_str + 'gstreamer' + regscanner, 'BUILDING gstreamer')
        run_cmd(port_str + 'gst-plugins-base', 'BUILDING gst-plugins-base')
        run_cmd(port_str + 'gst-plugins-good', 'BUILDING gst-plugins-good')
        run_cmd(port_str + 'gst-plugins-bad' + apple_media, 'BUILDING gst-plugins-bad')
        run_cmd(port_str + 'libmpeg2 +no_sdl', 'BUILDING libmpeg2')        
        run_cmd(port_str + 'gst-plugins-ugly', 'BUILDING gst-plugins-ugly')
        run_cmd(port_str + 'gst-ffmpeg', 'BUILDING gst-ffmpeg')
        if build_gstgl:
            run_cmd(port_str + 'gst-plugins-gl', 'BUILDING gst-plugins-gl')
        run_cmd(port_str + 'gnonlin', 'BUILDING gnonlin')
    else:
        run_cmd(port_str + 'gstreamer' + regscanner, 'BUILDING gstreamer')
        run_cmd(port_str + 'gst-plugins-base +no_x11 +no_gnome_vfs', 'BUILDING gst-plugins-base')
        run_cmd(port_str + 'gst-plugins-good +no_soup +no_keyring +no_caca', 'BUILDING gst-plugins-good')        
        run_cmd(port_str + 'gst-plugins-bad +no_x11 +no_glade +no_mms' + apple_media, 'BUILDING gst-plugins-bad')        
        run_cmd(port_str + 'libmpeg2 +no_x11 +no_sdl', 'BUILDING libmpeg2')
        run_cmd(port_str + 'gst-plugins-ugly', 'BUILDING gst-plugins-ugly')
        run_cmd(port_str + 'gst-ffmpeg', 'BUILDING gst-ffmpeg')
        if build_gstgl:        
            run_cmd(port_str + 'gst-plugins-gl', 'BUILDING gst-plugins-gl')
        run_cmd(port_str + 'gnonlin', 'BUILDING gnonlin')

def write_parameters(cpu_arch, target_os, install_dir, macports_ver, local_repos, use_x11, use_regscanner):
    global session_file
    
    session_file.write('arch=' + cpu_arch + '\n')
    session_file.write('target=10.' + str(target_os) + '\n')    
    session_file.write('dir=' + install_dir + '\n')
    session_file.write('macports=' + macports_ver + '\n')
    for lrepo in local_repos:
        session_file.write('lrepo=' + lrepo + '\n')
    if use_x11:
        session_file.write('X11=yes\n')
    else:
        session_file.write('X11=no\n')
    if use_regscanner:    
        session_file.write('regscanner=yes\n')
    else:
        session_file.write('regscanner=no\n')
        
def main():
    SUPP_ARCH = ['i386', 'x86_64']
    cpu_arch = 'x86_64'

    SUPP_TARGET = {'10.5':5, '10.6':6}
    target_os = 6
    
    local_repos = []    
    install_dir = '/opt/local'    
    macports_ver = '1.9.2'
    use_x11 = 0
    use_regscanner = 1

    global session_step
    session_step = 0 
    
    resume_mode = 0
    resume_file = './build_session'
        
    # Reading command line arguments
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
                        print_error('Wrong argument value (' + cpu_arch + ')')
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
                        print_error('Wrong argument value (' + val + ')')
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
            elif arg == '--lrepo':
                if i + 1 < l:
                    next_is_value = 1
                    local_repos.append(sys.argv[i+1])
                else:
                    print_error('Missing argument value')
                    return
            elif arg == '--step':        
                if i + 1 < l:
                    next_is_value = 1
                    session_step = int(sys.argv[i+1])
                else:
                    print_error('Missing argument value')
                    return                    
            elif arg == '-X11':
                use_x11 = 1                
            elif arg == '-noregscan':                
                use_regscanner = 0
            elif arg == '-resume':
                if os.path.lexists(resume_file):
                    resume_mode = 1 
                else:
                    print_error('Missing session file')
                    return     
            elif arg == '--help':
                print_help()
                return
            else:
                print_error('Unknown argument (' + arg + ')')
                return
    else:
        print_help()
        return
           
    if resume_mode:
        ifile = open(resume_file)
        lines = ifile.readlines()
        for line in lines:
            if line.strip() == '':
              continue
            parts = line.split('=')
            if len(parts) < 2:
                print_error('Broken session file')
                return 
            arg = parts[0].strip()
            val = parts[1].strip()
          
            if arg == 'arch':
                cpu_arch = val
                if not cpu_arch in SUPP_ARCH:
                    print_error('Wrong argument value (' + cpu_arch + ')')
                    return              
            elif arg == 'target':
                if val in SUPP_TARGET:
                    target_os = SUPP_TARGET[val]
                else:
                    print_error('Wrong argument value (' + val + ')')
                    return          
            elif arg == 'dir':
                install_dir = val
            elif arg == 'macports':
                macports_ver = val
            elif arg == 'lrepo':
                local_repos.append(val)
            elif arg == 'X11':
                if val == 'yes':
                    use_x11 = 1
                elif val == 'no':
                    use_x11 = 0
                else:          
                    print_error('Wrong argument value (' + val + ')')
                    return
            elif arg == 'regscanner':
                if val == 'yes':
                    use_regscanner = 1
                elif val == 'no':
                    use_regscanner = 0
                else:          
                    print_error('Wrong argument value (' + val + ')')
                    return                    
            elif arg == 'step':
                session_step = int(val)
            else:
                print_error('Unknown argument (' + arg + ')')
                return
                
        ifile.close()
    
    global session_file
    session_file = open(resume_file, 'w')
    write_parameters(cpu_arch, target_os, install_dir, macports_ver, local_repos, use_x11, use_regscanner)    
    
    macports_str = 'MacPorts-' + macports_ver
    macports_dir = './' + macports_str
    
    if session_step < 1:
        download_macports(macports_str, macports_dir)
        session_step = 1
        
    session_file.write('step=1\n')
    session_file.flush()     

    if session_step < 2:
        setup_macports(cpu_arch, macports_dir, install_dir)
        session_step = 2

    session_file.write('step=2\n')
    session_file.flush()     

    if session_step < 3:
        fix_ldflags(install_dir, target_os)
        session_step = 3

    session_file.write('step=3\n')
    session_file.flush()

    if session_step < 4:    
        fix_conf(install_dir, target_os, cpu_arch)
        session_step = 4

    session_file.write('step=4\n')
    session_file.flush()     
    
    if session_step < 5:
        delete_files()
        session_step = 5

    session_file.write('step=5\n')
    session_file.flush()     

    # Always done to catch an update in the local repositories (new patches, portfiles, etc)        
    add_local_repos(install_dir, local_repos)

    if session_step < 6:
        build_gstreamer(install_dir, cpu_arch, target_os, use_x11, use_regscanner)
        session_step = 6

    session_file.write('step=6\n')
    session_file.flush()

    session_file.close()
main()