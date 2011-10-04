#!/usr/bin/env python
# This script removes the dylibs that are not used. For example:                                                     
# removesymlinks.py --dir ./libs                                                  

import sys
import os
import subprocess

def print_help():
    print 'This script removes the dylibs that are not used. For example:                  '
    print 'removedeaddeps.py --dir ./libs                                                  '

def print_error(error):
    print error + '. Get some help by typing:'
    print 'removedeaddeps.py --help'     

# This function returns a dictionary where the full name of the library files in the directory
# dir are mapped to the shortest symlink name.
def generate_counts_map(dir):
    print 'Initializing dependency counts...'
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
            ext = os.path.splitext(fullname)[1]
            if ext == '.dylib':
                # Last occurrence of '-' (lib-test-0.10.dylib):
                n0 = fullname.rfind('-')
                # First occurrence of '.' (lib.1.2.dylib):
                n1 = fullname.find('.')
                if n0 == -1: n0 = 10000
                if n1 == -1: n1 = 10000
                n = min(n0, n1)
                name = fullname[0:n]
                map[fullname] = 0;

    return map

def process_file(counts, libname, fn, isdylib):
    print 'Counting dependencies in', os.path.basename(fn)
    pipe = subprocess.Popen('otool -L ' + fn, shell=True, stdout=subprocess.PIPE).stdout
    output = pipe.readlines()

    # Since the dylib files have the first line of the otool -L result containing their own name,
    # and the .so (and executable) files don't, we do this to take into account this thing.
    if isdylib:
        i = 1
    else:
        i = 0
       
    for line in output[1 + i:len(output)]:
        line = line.strip()
        fullname = line.split()[0]
        name = os.path.basename(fullname)
        if name in counts:
            counts[name] = counts[name] + 1


def process_dir(args, dirname, filelist):
    for filename in filelist:
        parts = os.path.splitext(filename)
        is_gst_tool = 0
        if -1 < filename.find('gst-'):
            is_gst_tool = 1        
        ext = parts[1]
        if ext == '.dylib' or ext == '.so' or is_gst_tool:
            fn = os.path.join(dirname, filename)
            process_file(args, filename, fn, ext == '.dylib')
    
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

    print 'WORKING...'
    counts_map = generate_counts_map(input_dir)    
    os.path.walk(input_dir, process_dir, counts_map)
    
    for key in counts_map:
        if counts_map[key] == 0:
            fn = os.path.join(input_dir, key) 
            print 'Deleting ' + fn
            proc = subprocess.Popen('rm ' + fn, shell=True)
            sts = os.waitpid(proc.pid, 0)[1]
            
    print 'DONE.'
main()