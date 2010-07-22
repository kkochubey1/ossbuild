@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set BUILD_WIN32=%TOP%\Build\Windows\Win32\Release\bin
set SHARED_WIN32=%TOP%\Shared\Build\Windows\Win32\bin

set GST_PLUGIN_PATH=%BUILD_WIN32%\plugins
set PATH=%BUILD_WIN32%;%SHARED_WIN32%;%PATH%


cd /d "%OLDDIR%"

start cmd.exe

cd /d "%OLDDIR%"