DESCRIPTION = "Soletta Development Application"
DEPENDS = "nodejs-native"
RDEPENDS_${PN} = "soletta nodejs systemd graphviz libmicrohttpd avahi-daemon bash git"
LICENSE = "BSD-3-Clause"
PV = "1_beta4"

LIC_FILES_CHKSUM = "file://${S}/LICENSE;md5=dbf9699ab0f60ec50f52ce70fcd07caf"

SRC_URI = "git://git@github.com/solettaproject/soletta-dev-app.git;protocol=https \
           file://soletta-dev-app.service \
           file://soletta-dev-app-mac.sh \
           file://soletta-dev-app-avahi-discover.service \
"
SRCREV = "5389b18efb75d21bcc593bb059917af27cb82b4b"

S = "${WORKDIR}/git"

# We provide only one package
PACKAGES = " \
	${PN} \
"

INSTALLATION_PATH = "/opt/"
SYSTEMD_PATH = "${systemd_unitdir}/system/"
AUTOSTART_SYSTEMD_PATH = "/etc/systemd/system/multi-user.target.wants/"
AVAHI_SERVICE = "/etc/avahi/services/"

FILES_${PN} += " \
    ${INSTALLATION_PATH}soletta-dev-app \
    ${SYSTEMD_PATH}soletta-dev-app-server.service \
    ${SYSTEMD_PATH}fbp-runner@.service \
    ${AUTOSTART_SYSTEMD_PATH}soletta-dev-app-server.service \
    ${AVAHI_SERVICE}soletta-dev-app.service \
    ${SYSTEMD_PATH}soletta-dev-app-avahi-discover.service \
    /soletta-dev-app/scripts/soletta-dev-app-mac.sh \
"

do_compile() {

    # changing the home directory to the working directory, the .npmrc will be created in this directory
    export HOME=${WORKDIR}

    # does not build dev packages
    npm config set dev false

    # access npm registry using http
    npm set strict-ssl false
    npm config set registry http://registry.npmjs.org/

    # configure http proxy if neccessary
    if [ -n "${http_proxy}" ]; then
        npm config set proxy ${http_proxy}
        export bower_https_proxy=${http_proxy}
    fi
    if [ -n "${HTTP_PROXY}" ]; then
        npm config set proxy ${HTTP_PROXY}
        export bower_https_proxy=${HTTP_PROXY}
    fi

    # configure cache to be in working directory
    npm set cache ${WORKDIR}/npm_cache

    # clear local cache prior to each compile
    npm cache clear

    case ${TARGET_ARCH} in
        i?86) targetArch="ia32"
            echo "targetArch = 32"
            ;;
        x86_64) targetArch="x64"
            echo "targetArch = 64"
            ;;
        arm) targetArch="arm"
            ;;
        mips) targetArch="mips"
            ;;
        sparc) targetArch="sparc"
            ;;
        *) echo "unknown architecture"
           exit 1
            ;;
    esac

    npm install --arch=${targetArch} --production --verbose -g bower@v1.6.8

    # compile and install node modules in source directory
    npm --arch=${targetArch} --production --verbose install

    bower -V install
}

do_install() {
  install -d ${D}{INSTALLATION_PATH}
  install -d ${D}${INSTALLATION_PATH}soletta-dev-app
  cp -r ${S}/* ${D}${INSTALLATION_PATH}soletta-dev-app

  #SYSTEMD Installation part
  install -d ${D}${SYSTEMD_PATH}
  install -m 0664 ${S}/scripts/units/fbp-runner@.service ${D}${SYSTEMD_PATH}
  install -m 0664 ${S}/scripts/units/soletta-dev-app-server.service.in ${D}${SYSTEMD_PATH}soletta-dev-app-server.service
  sed -i "s@PATH@"${INSTALLATION_PATH}soletta-dev-app"@" ${D}${SYSTEMD_PATH}soletta-dev-app-server.service
  sed -i "s@"NODE_BIN_NAME"@"node"@" ${D}${SYSTEMD_PATH}soletta-dev-app-server.service

  #Autostart soletta-dev-app-server service
  install -d ${D}${AUTOSTART_SYSTEMD_PATH}
  ln -sf ${SYSTEMD_PATH}soletta-dev-app-server.service ${D}${AUTOSTART_SYSTEMD_PATH}soletta-dev-app-server.service

  #Configure avahi to discover Soletta Dev App server
  install -d ${D}${AVAHI_SERVICE}
  install -m 0664 ${WORKDIR}/soletta-dev-app.service ${D}${AVAHI_SERVICE}

  #Configure services that will set MAC address to Soletta Dev-App name
  install -m 0664 ${WORKDIR}/soletta-dev-app-avahi-discover.service ${D}${SYSTEMD_PATH}
  ln -sf ${SYSTEMD_PATH}soletta-dev-app-avahi-discover.service ${D}${AUTOSTART_SYSTEMD_PATH}

 #Install set MAC address script
 install  -m 0755 ${WORKDIR}/soletta-dev-app-mac.sh ${D}${INSTALLATION_PATH}soletta-dev-app/scripts/

}
