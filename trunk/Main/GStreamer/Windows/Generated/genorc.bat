set MY_ORIGPATH=%PATH%
set MY_CURR=%CD%
set MY_DIR=%~dp0.
set MY_TOPDIR=%MY_DIR%\..\..\..\..
set MY_TOOLSDIR=%MY_TOPDIR%\Tools
set MY_SHAREDDIR=%MY_TOPDIR%\Shared
set MY_SHAREDBINDIR=%MY_SHAREDDIR%\Build\Windows\Win32\bin

set MY_ORCC=orcc.exe

set PATH=%MY_SHAREDBINDIR%;%MY_TOOLSDIR%;%MY_PYTHON_INSTALL_DIR%;%PATH%

set ORC_SOURCE=%1
set MY_FOLDER_INPUT=%2
set MY_FOLDER_OUTPUT=%3

cd %GENERATED_OUTPUT_DIR%
echo %GENERATED_OUTPUT_DIR%

echo Generating %MY_FOLDER_OUTPUT_DIR%\%ORC_SOURCE%.c
echo "%MY_ORCC% --implementation --include glib.h -o %MY_FOLDER_OUTPUT%\%ORC_SOURCE%.c %MY_FOLDER_INPUT%\%ORC_SOURCE%.orc"
%MY_ORCC% --implementation --include glib.h -o %MY_FOLDER_OUTPUT%\%ORC_SOURCE%.c %MY_FOLDER_INPUT%\%ORC_SOURCE%.orc

echo Generating %MY_FOLDER_OUTPUT_DIR%\%ORC_SOURCE%.h
%MY_ORCC% --header --include glib.h -o %MY_FOLDER_OUTPUT%\%ORC_SOURCE%.h %MY_FOLDER_INPUT%\%ORC_SOURCE%.orc

goto end

:end
set PATH=%MY_ORIGPATH%
cd /d "%MY_CURR%"
