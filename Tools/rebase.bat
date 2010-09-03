@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set TOOLS=%TOP%\Tools
set MSYS=%TOOLS%\msys

set PATH=%MSYS%\mingw\bin;%MSYS%\bin;%PATH%


cd /d "%MSYS%"

echo Rebasing executables/libraries...
dash "/bin/rebaseall"
echo Done.

cd /d "%OLDDIR%"