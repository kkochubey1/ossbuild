#!/usr/bin/env make

#
# Toolchain bootstrapping build system, ver. N + 1
#
# This exists as an attempt to unify building inside and outside of buildbot.
# Each step of the build system has an alias target that can be called
# externally; all steps _up_to_ the target will be executed.  If no target is
# specified, by default it will build a binary tarball (with matching source
# tarball) for 64bit Windows.
#
# The goal is to be able to replace makebuildroot.sh with all its options
# available.
#

all:: # default target

########################################
# Configurable variables
########################################
TARGET_ARCH ?= x86_64-w64-mingw32
HOST_ARCH ?= 
ALL_UPDATE ?= # force update everything
BINUTILS_VERSION ?= 2.20.51
BINUTILS_UPDATE ?= ${ALL_UPDATE} # force update binutils
BINUTILS_CONFIG_EXTRA_ARGS ?= --disable-debug
GCC_CONFIG_EXTRA_ARGS ?= --with-pkgversion="OSSBuild-v0.10.8, r164692" --with-bugurl="http://code.google.com/p/ossbuild/issues/" --disable-debug --enable-languages=c,c++,fortran,objc,obj-c++ --disable-multilib --enable-targets=all --disable-werror --enable-fully-dynamic-string --disable-nls --disable-win32-registry --enable-version-specific-runtime-libs --enable-libstdcxx-debug --enable-checking=release
GCC_BRANCH ?= branches/gcc-4_5-branch #trunk # "tags/gcc_4_4_0_release" or "branches/gcc-4_4-branch"
GCC_REVISION ?= 164692 #head # revision id "146782" or date "2009-04-25"
GCC_UPDATE ?= ${ALL_UPDATE} # force update gcc
GMP_VERSION ?= 5.0.1 # GMP release version
MPFR_VERSION ?= 2.4.2 # MPFR release version
MPC_VERSION ?= 0.8.2 # MPC release version
MINGW_BRANCH ?= trunk # ... not that we have any!
MINGW_REVISION ?= HEAD
MINGW_UPDATE ?= ${ALL_UPDATE} # force update mingw
MINGW_CONFIG_EXTRA_ARGS ?=
SRC_ARCHIVE ?= mingw-w64-x86_64-ossbuild-src.tar.bz2
BIN_ARCHIVE ?= mingw-w64-x86_64-ossbuild-bin.tar.bz2

########################################
# Configure
########################################

#Tools
#  CVS     Compression defaulted to 9
#  SVN     Default options for all svn commands
#  SVN_CO  Override this checkout command to "export" if desired
#  TAR     Prefer gnutar to tar (useful for darwin/bsdtar issues)
#  WGET    Try wget, fall back to curl
######

TAR := $(or $(shell which gnutar 2>/dev/null),$(shell which tar 2>/dev/null),tar)
PATCH ?= patch -u -N -i
CVS=cvs -z9
SVN=svn --non-interactive --no-auth-cache
SVN_CO=co
WGET=wget -c -t0 -T60 -O
ifeq (,$(shell which wget 2>/dev/null))
  WGET=curl -o
endif

ifeq (,$(filter-out x86_64-%,${TARGET_ARCH}))
  MINGW_LIBDIR := lib64
else ifeq (,$(filter-out i386-% i486-% i586-% i686-%,${TARGET_ARCH}))
  MIGNW_LIBDIR := lib
else
  $(error Unknown CPU for target arch ${TARGET_ARCH})
endif

# the type of _host_ to run on (n.b. not build)
HOST_TYPE := posix
ifneq (,$(filter MINGW%,$(shell uname -s)))
  HOST_TYPE := windows
endif
ifneq (,${HOST_ARCH})
  HOST_TYPE := posix
  BINUTILS_CONFIG_HOST_ARGS = --host=${HOST_ARCH}
  GCC_CONFIG_HOST_ARGS = --host=${HOST_ARCH}
endif
ifneq (,$(filter %-mingw32,${HOST_ARCH}))
  HOST_TYPE := windows
endif



########################################
# Pull mingw-w64-specific patches
########################################

patch-pull: \
    src/patches/.patches.pull.marker

src/patches/.patches.pull.marker: \
    src/patches/.mkdir.marker
	cd $(dir $@) && \
	$(SVN) $(SVN_CO) \
	       https://mingw-w64.svn.sourceforge.net/svnroot/mingw-w64/experimental/patches/ .
	@touch $@

########################################
# Download binutils
########################################

binutils-download: \
    src/binutils.tar.bz2

src/binutils.tar.bz2: \
    src/.mkdir.marker
	$(WGET) $@ http://mirrors.kernel.org/sources.redhat.com/binutils/snapshots/binutils-$(strip ${BINUTILS_VERSION}).tar.bz2

########################################
# Extract binutils
########################################

binutils-extract: \
    src/.binutils.extract.marker \
    src/binutils/.patch.marker

src/.binutils.extract.marker: \
    src/binutils.tar.bz2 \
    src/binutils/src/.mkdir.marker
	$(TAR) -C $(dir $@)/binutils/src --no-same-owner --strip-components=1 -xjvf $<
	@touch $@

binutils-patch:  \
    src/binutils/.patch.marker

src/binutils/.patch.marker: \
    src/patches/.patches.pull.marker \
    src/.binutils.extract.marker
	@touch $@

########################################
# Pull GCC
########################################
## find a gcc revision (r???) and a stamp for the file name ${GCC_REV_STAMP}
ifneq (,$(findstring -,${GCC_REVISION}))
  # GCC_REVISION is a date
  GCC_REV_STAMP := d$(subst -,,${GCC_REVISION})
  GCC_REVISION := $(shell TZ=Z $(SVN) log \
                                       -r "{$(subst -,,${GCC_REVISION})T0000Z}:{$(subst -,,${GCC_REVISION})T0030Z}" \
                                       svn://gcc.gnu.org/svn/gcc/trunk | \
                          grep gccadmin | \
                          grep ${GCC_REVISION} | \
                          cut -d ' ' -f 1)
  GCC_REVISION := $(subst r,,${GCC_REVISION})
else ifeq (_,$(if ${GCC_REVISION},,false)_$(strip \
              $(subst 0,, $(subst 1,, $(subst 2,, \
              $(subst 3,, $(subst 4,, $(subst 5,, \
              $(subst 6,, $(subst 7,, $(subst 8,, \
              $(subst 9,, $(subst 0,, ${GCC_REVISION} )))))))))))))
  # GCC_REVISION is a number
  GCC_REVISION := ${GCC_REVISION}
  GCC_REV_STAMP := r${GCC_REVISION}
else
  # GCC_REVISION is some opaque string (e.g. "head")
  GCC_REV_STAMP := r${GCC_REVISION}
endif # GCC revision

gcc-pull: \
    src/gcc/.gcc.pull.marker \
    src/gcc/.gcc.patch.marker

src/gcc/.gcc.pull.marker: \
    src/gcc/.mkdir.marker
	cd $(dir $@) && \
	$(SVN) $(SVN_CO) --revision ${GCC_REVISION} \
	       svn://gcc.gnu.org/svn/gcc/$(strip ${GCC_BRANCH})/ src
	### FIXME: Remove the next two lines!
	cd $(dir $@)src && \
	contrib/gcc_update --touch
	@touch $@

  ifneq (,$(strip ${GCC_UPDATE}))
.PHONY: src/gcc/.gcc.pull.marker
  endif

gcc-patch: \
    src/gcc/.gcc.patch.marker

src/gcc/.gcc.patch.marker: \
    src/patches/.patches.pull.marker \
    src/gcc/.gcc.pull.marker
	### -cd $(dir $@) && cd src \
	### $(PATCH) ../../patches/gcc/libgcc_multilib.patch
	@touch $@

########################################
# Download gmp
########################################

gmp-download: \
    src/gmp.tar.bz2

src/gmp.tar.bz2: \
    src/.mkdir.marker
	$(WGET) $@ ftp://ftp.gnu.org/gnu/gmp/gmp-$(strip ${GMP_VERSION}).tar.bz2

########################################
# Extract gmp
########################################

gmp-extract: \
    src/.gmp.extract.marker \
    src/gcc/src/gmp/.patch.marker

src/.gmp.extract.marker: \
    src/gmp.tar.bz2 \
    src/gcc/src/gmp/.mkdir.marker
	$(TAR) -C $(dir $@)/gcc/src/gmp --no-same-owner --strip-components=1 -xjvf $<
	@touch $@

gmp-patch:  \
    src/gcc/src/gmp/.patch.marker

src/gcc/src/gmp/.patch.marker: \
    src/patches/.patches.pull.marker \
    src/.gmp.extract.marker
	-cd $(dir $@) && \
	$(PATCH) ../../../patches/gmp/configure.patch
	@touch $@

########################################
# Download mpfr
########################################

mpfr-download: \
    src/mpfr.tar.bz2

src/mpfr.tar.bz2: \
    src/.mkdir.marker
	$(WGET) $@ http://www.mpfr.org/mpfr-$(strip ${MPFR_VERSION})/mpfr-$(strip ${MPFR_VERSION}).tar.bz2

########################################
# Extract mpfr
########################################

mpfr-extract: \
    src/.mpfr.extract.marker \
    src/gcc/src/mpfr/.patch.marker

src/.mpfr.extract.marker: \
    src/mpfr.tar.bz2 \
    src/gcc/src/mpfr/.mkdir.marker
	$(TAR) -C $(dir $@)/gcc/src/mpfr --no-same-owner --strip-components=1 -xjvf $<
	@touch $@

mpfr-patch: \
    src/gcc/src/mpfr/.patch.marker

src/gcc/src/mpfr/.patch.marker: \
    src/patches/.patches.pull.marker \
    src/.mpfr.extract.marker
	@touch $@

########################################
# Download mpc
########################################

mpc-download: \
    src/mpc.tar.gz

src/mpc.tar.gz: \
    src/.mkdir.marker
	$(WGET) $@ http://www.multiprecision.org/mpc/download/mpc-$(strip ${MPC_VERSION}).tar.gz

########################################
# Extract mpc
########################################

mpc-extract: \
    src/.mpc.extract.marker \
    src/gcc/src/mpc/.patch.marker

src/.mpc.extract.marker: \
    src/mpc.tar.gz \
    src/gcc/src/mpc/.mkdir.marker
	$(TAR) -C $(dir $@)/gcc/src/mpc --no-same-owner --strip-components=1 -xzvf $<
	@touch $@

mpc-patch: \
    src/gcc/src/mpc/.patch.marker

src/gcc/src/mpc/.patch.marker: \
    src/patches/.patches.pull.marker \
    src/.mpc.extract.marker
	-cd $(dir $@) && \
	$(PATCH) ../../../patches/mpc-0.8.2/0001-Fix-autotools-misuse.patch && \
	$(PATCH) ../../../patches/mpc-0.8.2/0002-Regenerate-files.patch
	@touch $@

########################################
# Pull mingw
########################################

mingw-pull: \
    src/mingw/.mingw.pull.marker

src/mingw/.mingw.pull.marker: \
    src/mingw/.mkdir.marker
	$(SVN) $(SVN_CO) --revision ${MINGW_REVISION} \
	    https://mingw-w64.svn.sourceforge.net/svnroot/mingw-w64/$(strip ${MINGW_BRANCH})/ \
	    $(dir $@)
	@touch $@

  ifneq (,$(strip ${MINGW_UPDATE}))
.PHONY: src/mingw/.mingw.pull.marker
  endif

## XXX Mook: stamp the source with the revision used

########################################
# Create source tarball
########################################

src-archive:  ${SRC_ARCHIVE}

ifeq (,$(wildcard ${SRC_ARCHIVE}))
${SRC_ARCHIVE}: \
    src/.binutils.extract.marker \
	src/binutils/.patch.marker \
    src/gcc/.gcc.pull.marker \
	src/gcc/.gcc.patch.marker \
    src/.gmp.extract.marker \
	src/gcc/src/gmp/.patch.marker \
    src/.mpfr.extract.marker \
	src/gcc/src/mpfr/.patch.marker \
    src/.mpc.extract.marker \
	src/gcc/src/mpc/.patch.marker \
    src/mingw/.mingw.pull.marker 
endif

${SRC_ARCHIVE}:
	$(TAR) vcjf $@ --owner 0 --group 0 --exclude=.svn \
	    --exclude=.*.marker --exclude=CVS --exclude=gmp.tar.bz2 \
	    --exclude=mpfr.tar.bz2 --exclude=mpc.tar.gz \-C src .

################################################################################
# Build
################################################################################

BUILD_DIR := build

########################################
# Extract source tarball
########################################
src-extract:: \
    build/.extract.marker

build/.extract.marker: \
    ${SRC_ARCHIVE}
	-mkdir -p $(dir $@)
	$(TAR) -C $(dir $@) -xvjpf $<
	@touch $@

${BUILD_DIR}/root/.root.init.marker: \
    ${BUILD_DIR}/root/${TARGET_ARCH}/.mkdir.marker \
    build/.extract.marker
ifneq (,$(filter MINGW%,$(shell uname -s)))
	test -e ${BUILD_DIR}/root/mingw  || \
	  junction ${BUILD_DIR}/root/mingw "${BUILD_DIR}/root/${TARGET_ARCH}"
	test -e ${BUILD_DIR}/root/mingw
else
	test -h ${BUILD_DIR}/root/mingw  || \
	  ln -s "${TARGET_ARCH}" ${BUILD_DIR}/root/mingw
	test -h ${BUILD_DIR}/root/mingw
endif
	@touch $@

########################################
# Configure mingw-w64 headers
########################################
headers-configure: \
    ${BUILD_DIR}/mingw-headers/obj/.config.marker

${BUILD_DIR}/mingw-headers/obj/.config.marker: \
    ${BUILD_DIR}/root/.root.init.marker \
    ${BUILD_DIR}/mingw-headers/obj/.mkdir.marker
	cd $(dir $@) && \
	  ${CURDIR}/${BUILD_DIR}/mingw/mingw-w64-headers/configure \
	  --prefix=${CURDIR}/${BUILD_DIR}/root \
	  --with-sysroot=${CURDIR}/${BUILD_DIR}/root \
	  --host=${TARGET_ARCH} \
	  --enable-sdk=directx
	@touch $@

########################################
# Install mingw-w64 headers
########################################
headers-install: \
    ${BUILD_DIR}/mingw-headers/obj/.install.marker

${BUILD_DIR}/mingw-headers/obj/.install.marker: \
    ${BUILD_DIR}/mingw-headers/obj/.config.marker
	make -C $(dir $@) install
	@touch $@

########################################
# Configure binutils
########################################
binutils-configure: \
    ${BUILD_DIR}/binutils/obj/.config.marker

${BUILD_DIR}/binutils/obj/.config.marker: \
    ${BUILD_DIR}/binutils/obj/.mkdir.marker \
    ${BUILD_DIR}/root/.root.init.marker
	cd $(dir $@) && \
	../../../build/binutils/src/configure \
	    --target=${TARGET_ARCH} \
	    ${BINUTILS_CONFIG_HOST_ARGS} \
	    --prefix=${CURDIR}/${BUILD_DIR}/root \
	    --with-sysroot=${CURDIR}/${BUILD_DIR}/root \
	    ${BINUTILS_CONFIG_EXTRA_ARGS}
	@touch $@

########################################
# Compile binutils
########################################
binutils-compile: \
    ${BUILD_DIR}/binutils/obj/.compile.marker

${BUILD_DIR}/binutils/obj/.compile.marker: \
    ${BUILD_DIR}/binutils/obj/.config.marker
	make -j3 -C $(dir $@)
	@touch $@

########################################
# Install binutils
########################################
binutils-install: \
    ${BUILD_DIR}/binutils/obj/.install.marker

${BUILD_DIR}/binutils/obj/.install.marker: \
    ${BUILD_DIR}/binutils/obj/.compile.marker
	make -C $(dir $@) install
	@touch $@

########################################
# GCC cross compiling support - winsup
########################################
gcc-winsup: \
    build/gcc/.winsup.marker

build/gcc/gcc/.winsup.marker: \
    ${BUILD_DIR}/.extract.marker \
    ${BUILD_DIR}/root/.root.init.marker
ifneq (,$(filter MINGW%,$(shell uname -s)))
	test -e build/gcc/src/winsup  || \
	  junction build/gcc/src/winsup "${BUILD_DIR}/root"
	test -e build/gcc/src/winsup
else
	test -h build/gcc/src/winsup  || \
	  ln -s "../../../${BUILD_DIR}/root" build/gcc/src/winsup
	test -h build/gcc/src/winsup
endif
	@touch $@

########################################
# Configure GCC
########################################
gcc-configure: \
    ${BUILD_DIR}/gcc/obj/.config.marker

ifneq (,$(filter %-mingw32,${HOST_ARCH}))
${BUILD_DIR}/gcc/obj/.config.marker: \
    ${BUILD_DIR}/gcc/src/.winsup.marker
endif

${BUILD_DIR}/gcc/obj/.config.marker: \
    ${BUILD_DIR}/gcc/obj/.mkdir.marker \
    ${BUILD_DIR}/binutils/obj/.install.marker \
    ${BUILD_DIR}/root/.root.init.marker
	cd $(dir $@) && \
	../../../build/gcc/src/configure \
	    --target=${TARGET_ARCH} \
	    ${GCC_CONFIG_HOST_ARGS} \
	    --prefix=${CURDIR}/${BUILD_DIR}/root \
	    --with-sysroot=${CURDIR}/${BUILD_DIR}/root \
	    ${GCC_CONFIG_EXTRA_ARGS}
	@touch $@

########################################
# Compile GCC stage 1
########################################
gcc-bootstrap-compile: \
    build/gcc/obj/.bootstrap.compile.marker

build/gcc/obj/.bootstrap.compile.marker: \
    build/gcc/obj/.config.marker \
    ${BUILD_DIR}/mingw-headers/obj/.install.marker
	found_asm=yes make -j3 -C $(dir $@) all-gcc
	@touch $@

########################################
# Install GCC stage 1
########################################
gcc-bootstrap-install: \
    build/gcc/obj/.bootstrap.install.marker

build/gcc/obj/.bootstrap.install.marker: \
    build/gcc/obj/.bootstrap.compile.marker
	make -C $(dir $@) install-gcc
	@touch $@

########################################
# Configure mingw-w64 CRT
########################################
crt-configure: \
    ${BUILD_DIR}/mingw/obj/.config.marker

${BUILD_DIR}/mingw/obj/.config.marker: \
    build/gcc/obj/.bootstrap.install.marker \
    ${BUILD_DIR}/mingw/obj/.mkdir.marker
	cd $(dir $@) && \
	PATH=$(realpath build/root/bin):$$PATH \
	../../../build/mingw/mingw-w64-crt/configure \
	    --host=${TARGET_ARCH} \
	    --prefix=${CURDIR}/${BUILD_DIR}/root \
	    --with-sysroot=${CURDIR}/${BUILD_DIR}/root \
	    ${MINGW_CONFIG_EXTRA_ARGS}
	@touch $@

########################################
# Compile mingw-w64 CRT
########################################
crt-compile: \
    ${BUILD_DIR}/mingw/obj/.compile.marker

${BUILD_DIR}/mingw/obj/.compile.marker: \
    ${BUILD_DIR}/mingw/obj/.config.marker
	PATH=$(realpath build/root/bin):$$PATH \
	make -j3 -C $(dir $@)
	@touch $@

########################################
# Install mingw-w64 CRT
########################################
crt-install: \
    ${BUILD_DIR}/mingw/obj/.install.marker

${BUILD_DIR}/mingw/obj/.install.marker: \
    ${BUILD_DIR}/mingw/obj/.compile.marker
	PATH=$(realpath build/root/bin):$$PATH \
	make -C $(dir $@) install
	@touch $@

########################################
# Compile full GCC
########################################
gcc-compile: \
    ${BUILD_DIR}/gcc/obj/.compile.marker \
    ${BUILD_DIR}/mingw/obj/.install.marker

${BUILD_DIR}/gcc/obj/.compile.marker: \
    ${BUILD_DIR}/gcc/obj/.config.marker \
    ${BUILD_DIR}/mingw/obj/.install.marker
	PATH=$(realpath build/root/bin):$$PATH \
	make -j3 -C $(dir $@)
	@touch $@

########################################
# Install full GCC
########################################
gcc-install: \
    ${BUILD_DIR}/gcc/obj/.install.marker

${BUILD_DIR}/gcc/obj/.install.marker: \
    ${BUILD_DIR}/gcc/obj/.compile.marker
	PATH=$(realpath build/root/bin):$$PATH \
	make -j3 -C $(dir $@) install
	@touch $@

########################################
# Create release tarball
########################################
release-archive: \
    ${BIN_ARCHIVE}

${BIN_ARCHIVE}: \
    ${BUILD_DIR}/gcc/obj/.install.marker
	$(TAR) vcjf $@ -C ${BUILD_DIR}/root --owner 0 --group 0 \
	    --exclude=CVS --exclude=.svn --exclude=.*.marker --exclude=*.la \
            .

################################################################################
# Native (only active when native_dir != build_dir)
################################################################################

NATIVE_DIR := native

ifneq (${NATIVE_DIR},${BUILD_DIR})

########################################
# Initialize build root
########################################

${NATIVE_DIR}/root/.root.init.marker: \
    ${NATIVE_DIR}/root/${TARGET_ARCH}/.mkdir.marker \
    ${BUILD_DIR}/.extract.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Configure mingw-w64 headers
########################################
native-headers-configure: \
    ${NATIVE_DIR}/mingw-headers/obj/.config.marker

${NATIVE_DIR}/mingw-headers/obj/.config.marker: \
    ${NATIVE_DIR}/root/.root.init.marker \
    ${NATIVE_DIR}/mingw-headers/obj/.mkdir.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Install mingw-w64 headers
########################################
native-headers-install: \
    ${NATIVE_DIR}/mingw-headers/obj/.install.marker

${NATIVE_DIR}/mingw-headers/obj/.install.marker: \
    ${NATIVE_DIR}/mingw-headers/obj/.config.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Configure binutils
########################################
native-binutils-configure: \
    ${NATIVE_DIR}/binutils/obj/.config.marker

${NATIVE_DIR}/binutils/obj/.config.marker: \
    ${BUILD_DIR}/gcc/obj/.install.marker \
    ${NATIVE_DIR}/binutils/obj/.mkdir.marker \
    ${NATIVE_DIR}/root/.root.init.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Compile binutils
########################################
native-binutils-compile: \
    ${NATIVE_DIR}/binutils/obj/.compile.marker

${NATIVE_DIR}/binutils/obj/.compile.marker: \
    ${NATIVE_DIR}/binutils/obj/.config.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Install binutils
########################################
native-binutils-install: \
    ${NATIVE_DIR}/binutils/obj/.install.marker

${NATIVE_DIR}/binutils/obj/.install.marker: \
    ${NATIVE_DIR}/binutils/obj/.compile.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# GCC cross compiling support - winsup
########################################
native-gcc-winsup: \
    ${NATIVE_DIR}/gcc/src/.winsup.marker

${NATIVE_DIR}/gcc/src/.winsup.marker: \
    build/.extract.marker \
    ${NATIVE_DIR}/gcc/src/.mkdir.marker \
    ${NATIVE_DIR}/root/.root.init.marker
ifneq (,$(filter MINGW%,$(shell uname -s)))
	-test -e build/gcc/src/winsup  && \
	  junction -d build/gcc/src/winsup
	junction build/gcc/src/winsup "${NATIVE_DIR}/root"
	test -e build/gcc/src/winsup
else
	-test -h build/gcc/src/winsup && \
	  rm build/gcc/src/winsup
	ln -s "../../../${NATIVE_DIR}/root" build/gcc/src/winsup
	test -h build/gcc/src/winsup
endif
	@touch $@

########################################
# Configure GCC
########################################
native-gcc-configure: \
    ${NATIVE_DIR}/gcc/obj/.config.marker

${NATIVE_DIR}/gcc/obj/.config.marker: \
    ${NATIVE_DIR}/gcc/src/.winsup.marker \
    ${NATIVE_DIR}/gcc/obj/.mkdir.marker \
    ${NATIVE_DIR}/binutils/obj/.install.marker \
    ${NATIVE_DIR}/root/.root.init.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Configure mingw-w64 CRT
########################################
native-crt-configure: \
    ${NATIVE_DIR}/mingw/obj/.config.marker

${NATIVE_DIR}/mingw/obj/.config.marker: \
    build/gcc/obj/.bootstrap.install.marker \
    ${NATIVE_DIR}/mingw/obj/.mkdir.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Compile mingw-w64 CRT
########################################
native-crt-compile: \
    ${NATIVE_DIR}/mingw/obj/.compile.marker

${NATIVE_DIR}/mingw/obj/.compile.marker: \
    ${NATIVE_DIR}/mingw/obj/.config.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Install mingw-w64 CRT
########################################
native-crt-install: \
    ${NATIVE_DIR}/mingw/obj/.install.marker

${NATIVE_DIR}/mingw/obj/.install.marker: \
    ${NATIVE_DIR}/mingw/obj/.compile.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Compile full GCC
########################################
native-gcc-compile: \
    ${NATIVE_DIR}/gcc/obj/.compile.marker \
    ${NATIVE_DIR}/mingw/obj/.install.marker

${NATIVE_DIR}/gcc/obj/.compile.marker: \
    ${NATIVE_DIR}/gcc/obj/.config.marker \
    ${NATIVE_DIR}/mingw/obj/.install.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Install full GCC
########################################
native-gcc-install: \
    ${NATIVE_DIR}/gcc/obj/.install.marker

${NATIVE_DIR}/gcc/obj/.install.marker: \
    ${NATIVE_DIR}/gcc/obj/.compile.marker
	PATH=$(realpath build/root/bin):$$PATH \
	${MAKE} -f $(lastword ${MAKEFILE_LIST}) \
	     HOST_ARCH=${TARGET_ARCH} \
	     TARGET_ARCH=${TARGET_ARCH} \
	     BUILD_DIR=${NATIVE_DIR} $@

########################################
# Create release tarball
########################################
native-release-archive: \
    native-${BIN_ARCHIVE}

native-${BIN_ARCHIVE}: \
    ${NATIVE_DIR}/gcc/obj/.install.marker
	cd ${NATIVE_DIR}/root && \
	zip -r -9 ${CURDIR}/$(patsubst %.tar.bz2,%.zip,$@) \
	     . -x .*.marker *.*.marker *.la


endif # native_dir != build_dir

################################################################################
# Helper targets
################################################################################

%/.mkdir.marker:
	-mkdir -p $(dir $@)
	@touch -t 197001020101 $@

help::
	@echo Available targets:
	@echo -e $(foreach t,all ${TARGETS} $@,\\t${t}\\n)

# build only the cross-compiler by default
all:: \
  ${BIN_ARCHIVE}

TARGETS := \
  patch-pull \
  binutils-extract \
  binutils-patch \
  gcc-pull \
  gcc-patch \
  gmp-download \
  gmp-extract \
  gmp-patch \
  mpfr-download \
  mpfr-extract \
  mpfr-patch \
  mpc-download \
  mpc-extract \
  mpc-patch \
  mingw-pull \
  src-archive \
  src-extract \
  binutils-configure \
  binutils-compile \
  binutils-install \
  gcc-configure \
  gcc-bootstrap-compile \
  gcc-bootstrap-install \
  headers-configure \
  headers-install \
  crt-configure \
  crt-compile \
  crt-install \
  gcc-compile \
  gcc-install \
  release-archive \
  native-binutils-configure \
  native-binutils-compile \
  native-binutils-install \
  native-gcc-configure \
  native-headers-configure \
  native-headers-install \
  native-crt-configure \
  native-crt-compile \
  native-crt-install \
  native-gcc-compile \
  native-gcc-install \
  native-release-archive \
  ${NULL}


.PHONY: \
  all \
  ${TARGETS} \
  help \
  ${NULL}
