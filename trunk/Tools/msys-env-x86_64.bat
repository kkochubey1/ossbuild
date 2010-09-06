@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set TOOLS=%TOP%\Tools


cd /d "%TOOLS%"

echo Creating MSys x86_64 build environment...
call msys-env.bat x86_64
echo Done.

cd /d "%OLDDIR%"