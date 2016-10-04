#! /bin/bash
MAC_ADDRESS=`ip addr show scope global | grep "link/ether" -m 1 | awk  -F' ' '{print $2}'`
sed -i "s@MACADDR@$MAC_ADDRESS@" /etc/avahi/services/soletta-dev-app.service
