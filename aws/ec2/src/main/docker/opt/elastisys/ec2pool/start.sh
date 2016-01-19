#!/bin/bash

set -e

# detect java location
JAVA_HOME="$(readlink -f /usr/bin/java | sed "s:/jre/bin/java::")"
java="${JAVA_HOME}/bin/java"

# where log output files are written
LOG_DIR=${LOG_DIR:-/var/log/elastisys/ec2pool}
# where runtime state is kept
STORAGE_DIR=${STORAGE_DIR:-/var/lib/elastisys/ec2pool}

logconfig="/etc/elastisys/ec2pool/logback.xml"
julconfig="/etc/elastisys/ec2pool/logging.properties"
security_dir="/etc/elastisys/security"
httpsport=443

# Java system properties
JVM_OPTS=${JVM_OPTS:--Xmx128m}
JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=${julconfig} -Dlogback.configurationFile=${logconfig}"
JAVA_OPTS="${JAVA_OPTS} -DLOG_DIR=${LOG_DIR}"

SECURITY_OPTS="--ssl-keystore ${security_dir}/server_keystore.p12 --ssl-keystore-password serverpassword"

# require clients to authenticate with a trusted certificate
# SECURITY_OPTS="${SERVER_OPTS} --ssl-truststore ${security_dir}/server_truststore.jks --ssl-truststore-password serverpassword --require-cert"

SERVER_OPTS="--storage-dir ${STORAGE_DIR} --https-port ${httpsport} ${SECURITY_OPTS}"

# to allow use of ports below 1024, use: authbind --deep <prog> [args ...]
# note: assumes that /etc/authbind/byport/<port> files with proper owner/mode
# have been set up
authbind --deep ${java} ${JVM_OPTS} ${JAVA_OPTS} -jar ec2pool.jar ${SERVER_OPTS}
