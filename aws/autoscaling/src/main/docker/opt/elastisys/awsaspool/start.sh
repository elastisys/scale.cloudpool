#!/bin/bash

set -e

# detect java location
JAVA_HOME="$(readlink -f /usr/bin/java | sed "s:/jre/bin/java::")"
java="${JAVA_HOME}/bin/java"

# where log output files are written
if [ "${LOG_DIR}" = "" ]; then
    LOG_DIR="/var/log/elastisys/awsaspool"
fi

# where runtime state is kept
if [ "${STORAGE_DIR}" = "" ]; then
    STORAGE_DIR="/var/lib/elastisys/awsaspool"
fi


logconfig="/etc/elastisys/awsaspool/logback.xml"
julconfig="/etc/elastisys/awsaspool/logging.properties"
security_dir="/etc/elastisys/security"
httpsport=443

# Java system properties
JAVA_OPTS="-Xmx64m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=${julconfig} -Dlogback.configurationFile=${logconfig}"
JAVA_OPTS="${JAVA_OPTS} -DLOG_DIR=${LOG_DIR}"

SECURITY_OPTS="--ssl-keystore ${security_dir}/server_keystore.p12 --ssl-keystore-password serverpassword"

# require clients to authenticate with a trusted certificate
# SECURITY_OPTS="${SERVER_OPTS} --ssl-truststore ${security_dir}/server_truststore.jks --ssl-truststore-password serverpassword --require-cert"

SERVER_OPTS="--storage-dir ${STORAGE_DIR} --https-port ${httpsport} ${SECURITY_OPTS}"

# to allow use of ports below 1024, use: authbind --deep <prog> [args ...]
# note: assumes that /etc/authbind/byport/<port> files with proper owner/mode
# have been set up
authbind --deep ${java} ${JAVA_OPTS} -jar awsaspool.jar ${SERVER_OPTS}
