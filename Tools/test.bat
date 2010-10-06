@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set TOOLS=%TOP%\Tools
set BUILD_WIN32=%TOP%\Build\Windows\Win32\Release\bin
set SHARED_WIN32=%TOP%\Shared\Build\Windows\Win32\bin

set MSYS=%TOOLS%\msys

set GST_REGISTRY=%BUILD_WIN32%\gst-registry.bin
set GST_PLUGIN_PATH=%BUILD_WIN32%\plugins
set PATH=%BUILD_WIN32%;%SHARED_WIN32%;%MSYS%\mingw\bin;%MSYS%\bin;%PATH%


cd /d "%OLDDIR%"

start cmd.exe

cd /d "%OLDDIR%"