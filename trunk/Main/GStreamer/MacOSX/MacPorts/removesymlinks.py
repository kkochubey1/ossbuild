#!/usr/bin/env python
# This script removes the symlinks to dylib files inside the specified directory,
# and renaming the actual dylib files to have the name of the symlink with the   
# shortest name. For example:                                                     
# removesymlinks.py --dir ./libs                                                  

import sys
import os
import subprocess

def print_help():
    print 'This script removes the symlinks to dylib files inside the specified directory, '
    print 'and renaming the actual dylib files to have the name of the symlink with the    '
    print 'shortest name. For example:                                                     '   
    print 'removesymlinks.py --dir ./libs                                                  '

def print_error(error):
    print error + '. Get some help by typing:'
    print 'removesymlinks.py --help'     

def add_to_map(map, list):
    l = len(list)
    if 0 < l:
        # We get the shortest name in the list
        nmin = -1
        n = 0
        lmin = 1000
        for s in list:
            l0 = len(s)
            if l0 < lmin:
                nmin = n
            n = n + 1
        shortest_name = list[nmin]
        
        # Now we create (name, shortest name) key-value pairs.
        for s in list:
            if s != shortest_name:
                map[s] = shortest_name

# This function returns a dictionary where the full name of the library files in the directory
# dir are mapped to the shortest symlink name.
def generate_link_map(dir):
    map = {}

    # The ls -l command will give a list in the following format:
    # -rwxr-xr-x  1 andres  staff   24224 Aug 10 09:59 libogg.0.dylib
    # lrwxr-xr-x  1 andres  staff      14 Aug 10 09:55 libogg.dylib -> libogg.0.dylib
    # -rwxr-xr-x  1 andres  staff  189856 Aug 10 09:59 libvorbis.0.dylib
    # lrwxr-xr-x  1 andres  staff      17 Aug 10 09:55 libvorbis.dylib -> libvorbis.0.dylib
    # -rwxr-xr-x  1 andres  staff   83492 Aug 10 09:59 libz.1.2.5.dylib
    # lrwxr-xr-x  1 andres  staff      16 Aug 10 09:55 libz.1.dylib -> libz.1.2.5.dylib
    # lrwxr-xr-x  1 andres  staff      16 Aug 10 09:55 libz.dylib -> libz.1.2.5.dylib
    pipe = subprocess.Popen('ls -l ' + dir, shell=True, stdout=subprocess.PIPE).stdout
    output = pipe.readlines()

    current_name = ''
    name_list = []
    for line in output:
        parts = line.split()
        if 9 <= len(parts) and line[0] != 'd':
            fullname = parts[8]
            # Last occurrence of '-' (lib-test-0.10.dylib):
            n0 = fullname.rfind('-')
            # First occurrence of '.' (lib.1.2.dylib):
            n1 = fullname.find('.')
            if n0 == -1: n0 = 10000
            if n1 == -1: n1 = 10000
            n = min(n0, n1)
            name = fullname[0:n]             
            if name != current_name:
                add_to_map(map, name_list)
                current_name = name
                name_list = []
            if line[0] == 'l':
                # File is a link, deleting.
                proc = subprocess.Popen('rm ' + dir + '/' + fullname, shell=True)
                sts = os.waitpid(proc.pid, 0)[1]             
            name_list = name_list + [fullname]
            
    add_to_map(map, name_list)

    #for key in map:
    #    print key, map[key]
    
    return map

def process_lib(map, libname, fn, isdylib):
    #print 'processing',fn
    pipe = subprocess.Popen('otool -L ' + fn, shell=True, stdout=subprocess.PIPE).stdout
    output = pipe.readlines()

    # Since the dylib files have the first line of the otool -L result containing its own name,
    # and the so files don't, we do this to take into account this thing.
    if isdylib:
        i = 0
    else:
       i = 1
       
    for line in output[1:len(output)]:
        line = line.strip()
        fullname = line.split()[0]
        name = os.path.basename(fullname)
        if name in map and map[name] != name:
            fullname_new = fullname.replace(name, map[name])
            if i == 0:
                # Changing name of dylib.
                proc = subprocess.Popen('install_name_tool -id ' + fullname_new + ' ' + fn, shell=True)
                sts = os.waitpid(proc.pid, 0)[1]
            else:            
                # Changing name of dependencies.
                proc = subprocess.Popen('install_name_tool -change ' + fullname + ' ' + fullname_new + ' ' + fn, shell=True)
                sts = os.waitpid(proc.pid, 0)[1]
                
        i = i + 1
        
    # Checking replacement succcess:
    pipe = subprocess.Popen('otool -L ' + fn, shell=True, stdout=subprocess.PIPE).stdout
    output = pipe.readlines()
    for line in output[1:len(output)]:
        line = line.strip()
        fullname = line.split()[0]
        name = os.path.basename(fullname)      
        if name in map and map[name] != name:
            print "Replacement failed:", fn, fullname

    # Now renaming library file to the name of the shortest link.
    name = os.path.basename(fn)      
    if name in map and map[name] != name:    
        fn_new = fn.replace(name, map[name])
        proc = subprocess.Popen('mv ' + fn + ' ' + fn_new, shell=True)
        sts = os.waitpid(proc.pid, 0)[1]

def process_dir(args, dirname, filelist):
    for filename in filelist:
        ext = os.path.splitext(filename)[1]
        if ext == '.dylib' or ext == '.so':
            fn = os.path.join(dirname, filename)
            process_lib(args, filename, fn, ext == '.dylib')
    
def main():
    input_dir = ''

    next_is_value = 0
    l = len(sys.argv)
    if 1 < l:
        for i in range(1, l):
            if next_is_value:
                next_is_value = 0 
                continue
            arg = sys.argv[i]
            if arg == '--dir':
                if i + 1 < l:
                    next_is_value = 1
                    input_dir = sys.argv[i+1]
                else:
                    print_error('Missing argument value')
                    return
            elif arg == '--help':
                print_help()
                return
            else:
                print_error('Unknown argument')
                return
    else:
        print_help()
        return

    print 'Working...'
    link_map = generate_link_map(input_dir)
    os.path.walk(input_dir, process_dir, link_map)  
    print 'Done.'
main()