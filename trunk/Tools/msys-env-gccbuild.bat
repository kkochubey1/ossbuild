@echo off

set OLDDIR=%CD%
set MYDIR=%~dp0
set TOP=%MYDIR%..
set TOOLS=%TOP%\Tools


cd /d "%TOOLS%"

echo Creating MSys GCC build environment...
call msys-env.bat gccbuild
echo Done.

cd /d "%OLDDIR%"