@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set TOOLS=%TOP%\Tools


cd /d "%TOOLS%"

echo Creating MSys x86 build environment...
call msys-env.bat x86
echo Done.

cd /d "%OLDDIR%"