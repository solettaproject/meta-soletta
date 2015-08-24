#
# soletta.bb
#

DESCRIPTION = "Soletta library and modules"
SECTION = "examples"
DEPENDS = "glib-2.0 libpcre python3-jsonschema"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://COPYING;md5=53eeaddf328b23e2355816e257450eaa"
PV = "0.0+git${SRCPV}"
SRCREV = "${AUTOREV}"

SRC_URI = "git://github.com/solettaproject/soletta.git;protocol=git;branch=master"

#Kbuild config file
SRC_URI += " file://config"

S = "${WORKDIR}/git"

inherit cml1

PACKAGES = " \
         ${PN}-staticdev \
         ${PN}-dev \
         ${PN}-dbg \
         ${PN} \
"

FILES_${PN}-staticdev = " \
                      ${libdir}/libsoletta.a \
"

FILES_${PN}-dbg = " \
                 ${datadir}/gdb \
"

FILES_${PN}-dev = " \
                ${datadir}/soletta/* \
                ${includedir}/soletta/* \
                ${libdir}/pkgconfig/soletta.pc \
                ${libdir}/soletta/modules/flow/* \
                ${libdir}/soletta/modules/pin-mux/* \
                ${libdir}/soletta/modules/linux-micro/* \
"

FILES_${PN} = " \
            ${bindir}/sol* \
            ${libdir}/libsoletta.so* \
"

# Setup what PACKAGES should be installed by default.
# If a package should not being installed, use BAD_RECOMMENDS.
RRECOMMENDS_${PN} = "\
                  ${PN} \
                  ${PN}-dev \
"

# since we only enable flow-module-udev only with systemd feature
# can can disable the RDEPENDS based on the same criteria
RDEPENDS_${PN} = " \
             ${@bb.utils.contains('DISTRO_FEATURES','systemd','libudev','',d)} \
             chrpath \
             libpcre \
"

# do_package_qa tells soletta rdepends on soletta-dev
# maybe an non-obvious implicit rule implied by yocto
INSANE_SKIP_${PN} += "dev-deps file-rdeps"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_DEFAULT_DEPS = "1"

B = "${WORKDIR}/git"

do_configure_prepend() {
   export TARGETCC="${CC}"
   export TARGETAR="${AR}"
   #Depending in what features are enabled, We must change some configurations.
   if [ "${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'systemd', '', d)}" = "systemd" ]; then
      echo "HAVE_SYSTEMD=y" >> ${WORKDIR}/config
      echo "HAVE_UDEV=n" >> ${WORKDIR}/config
   else
      echo "HAVE_SYSTEMD=n" >> ${WORKDIR}/config
      echo "HAVE_UDEV=y" >> ${WORKDIR}/config
   fi
   cp ${WORKDIR}/config ${B}/.config
}

do_compile() {
   oe_runmake CFLAGS="--sysroot=${STAGING_DIR_TARGET} -pthread -lpcre" TARGETCC="${CC}" TARGETAR="${AR}"
}

do_install() {
   oe_runmake DESTDIR=${WORKDIR}/image install CFLAGS="--sysroot=${STAGING_DIR_TARGET}" TARGETCC="${CC}" TARGETAR="${AR}"
   unlink ${WORKDIR}/image/usr/lib/libsoletta.so
   mv ${WORKDIR}/image/usr/lib/libsoletta.so.0.0.1 ${WORKDIR}/image/usr/lib/libsoletta.so
   ln -sf libsoletta.so ${WORKDIR}/image/usr/lib/libsoletta.so.0.0.1
}
