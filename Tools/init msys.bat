@echo off

REM **********************************************************************
REM * Creates a suitable MSys/MinGW environment for building the Windows 
REM * libraries using GCC and associated auto tools.
REM **********************************************************************

set DOWNLOAD=1
set UNTAR=1

set DIR=%~dp0.
set CURRDIR=%CD%
set TMPDIR=%DIR%\tmp
set MSYSDIR=%DIR%\msys-tmp
set MINGWDIR=%MSYSDIR%\mingw
set TOOLSDIR=%DIR%
set MINGW_GET_DEFAULTS=%TMPDIR%\var\lib\mingw-get\data\defaults.xml

set GCCURL=http://komisar.gin.by/mingw/cross-mingw.gcc450.generic.20100612.7z
set GCCDIR=cross-mingw.gcc450.generic.20100612

set PKG_MSYS_CORE=http://downloads.sourceforge.net/project/mingw/MSYS/BaseSystem/msys-1.0.14-1/msysCORE-1.0.14-1-msys-1.0.14-bin.tar.lzma?use_mirror=softlayer
set PKG_MSYS_RXVT=http://downloads.sourceforge.net/project/mingw/MSYS/rxvt/rxvt-2.7.2-3/rxvt-2.7.2-3-msys-1.0.14-bin.tar.lzma?use_mirror=voxel
set PKG_MSYS_BASH=http://downloads.sourceforge.net/project/mingw/MSYS/bash/bash-3.1.17-3/bash-3.1.17-3-msys-1.0.13-bin.tar.lzma?use_mirror=hivelocity
set PKG_MSYS_REGEX=http://downloads.sourceforge.net/project/mingw/MSYS/regex/regex-1.20090805-2/libregex-1.20090805-2-msys-1.0.13-dll-1.tar.lzma?use_mirror=iweb
set PKG_MSYS_TERMCAP_DLL=http://downloads.sourceforge.net/project/mingw/MSYS/termcap/termcap-0.20050421_1-2/libtermcap-0.20050421_1-2-msys-1.0.13-dll-0.tar.lzma?use_mirror=iweb

set PATH=%TMPDIR%\bin;%TOOLSDIR%;%PATH%

mkdir "%TMPDIR%"
cd /d "%TMPDIR%"


if "%DOWNLOAD%" == "1" (
	cd /d "%TMPDIR%"
	wget --no-check-certificate -O mingw-get.tar.gz "http://downloads.sourceforge.net/project/mingw/Automated MinGW Installer/mingw-get/mingw-get-0.1-mingw32-alpha-2-bin.tar.gz?use_mirror=softlayer"
	cd "%MSYSDIR%"
)

if "%UNTAR%" == "1" (
	cd /d "%TMPDIR%"
	7z -y x mingw-get.tar.gz
	7z -y x mingw-get.tar
	del mingw-get.tar
	del mingw-get.tar.gz
	cd "%MSYSDIR%"
)

mkdir "%MSYSDIR%"
mkdir "%MINGWDIR%"

rem Outputs the following:
rem 
rem <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
rem <profile project="MinGW" application="mingw-get">
rem     <repository uri="http://prdownloads.sourceforge.net/mingw/%F.xml.lzma?download">
rem     </repository>
rem     <system-map id="default">
rem     <sysroot subsystem="mingw32" path="c:/MinGW" />
rem         <sysroot subsystem="MSYS" path="c:/MSYS/1.0" />
rem     </system-map>
rem </profile>
echo %MINGW_GET_DEFAULTS%
echo ^<?xml version="1.0" encoding="UTF-8" standalone="yes"?^>                                              > %MINGW_GET_DEFAULTS%
echo ^<!-- --^>    ^<profile project="MinGW" application="mingw-get"^>                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<repository uri="http://prdownloads.sourceforge.net/mingw/%%F.xml.lzma?download"^> >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^</repository^>                                                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<system-map id="default"^>                                                         >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^<sysroot subsystem="mingw32" path="%MINGWDIR%" /^>                                 >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>            ^<sysroot subsystem="MSYS" path="%MSYSDIR%" /^>                                 >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>        ^</system-map^>                                                                     >> %MINGW_GET_DEFAULTS%
echo ^<!-- --^>    ^</profile^>                                                                            >> %MINGW_GET_DEFAULTS%

rem mingw-get install gcc

cd "%MSYSDIR%"
if "%DOWNLOAD%" == "1" (
	cd "%TMPDIR%"
	wget --no-check-certificate -O gcc.7z "http://komisar.gin.by/mingw/cross-mingw.gcc450.generic.20100612.7z"
	cd "%MSYSDIR%"
)

if "%UNTAR%" == "1" (
	cd "%TMPDIR%"
	
	7z -y x gcc.7z
	del gcc.7z
	cd "%GCCDIR%"
	xcopy /Y /K /H /E ".\*" "%MINGWDIR%"
	cd ..

	wget --no-check-certificate -O msysCore.tar.lzma "%PKG_MSYS_CORE%"
	7za -y x msysCore.tar.lzma
	7za -y "-o%MSYSDIR%" x msysCore.tar
	
	wget --no-check-certificate -O rxvt.tar.lzma "%PKG_MSYS_RXVT%"
	7za -y x rxvt.tar.lzma
	7za -y "-o%MSYSDIR%" x rxvt.tar
	
	wget --no-check-certificate -O bash.tar.lzma "%PKG_MSYS_BASH%"
	7za -y x bash.tar.lzma
	7za -y "-o%MSYSDIR%" x bash.tar
	
	wget --no-check-certificate -O regex.tar.lzma "%PKG_MSYS_REGEX%"
	7za -y x regex.tar.lzma
	7za -y "-o%MSYSDIR%" x regex.tar
	
	wget --no-check-certificate -O termcap.tar.lzma "%PKG_MSYS_TERMCAP_DLL%"
	7za -y x termcap.tar.lzma
	7za -y "-o%MSYSDIR%" x termcap.tar
	
	
	
	
	
	cd "%MSYSDIR%"
)


rem ???



:done
cd /d "%CURRDIR%"
rem exit 0
