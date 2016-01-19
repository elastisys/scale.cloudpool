#!/bin/bash

set -e

# detect java location
JAVA_HOME="$(readlink -f /usr/bin/java | sed "s:/jre/bin/java::")"
java="${JAVA_HOME}/bin/java"

# Logging
LOG_CONFIG=${LOG_CONFIG:-/etc/elastisys/awsaspool/logback.xml}
JUL_CONFIG=${JUL_CONFIG:-/etc/elastisys/awsaspool/logging.properties}
# log destination dir for default LOG_CONFIG
LOG_DIR=${LOG_DIR:-/var/log/elastisys/awsaspool}

# where runtime state is kept
STORAGE_DIR=${STORAGE_DIR:-/var/lib/elastisys/awsaspool}

# TLS/SSL settings
HTTPS_PORT=${HTTPS_PORT:-443}
SSL_KEYSTORE=${SSL_KEYSTORE:-/etc/elastisys/security/server_keystore.p12}
SSL_KEYSTORE_PASSWORD=${SSL_KEYSTORE_PASSWORD:-serverpassword}
# basic authentication
REQUIRE_BASIC_AUTH=${REQUIRE_BASIC_AUTH:-false}
BASIC_AUTH_ROLE=${BASIC_AUTH_ROLE:-USER}
BASIC_AUTH_REALM_FILE=${BASIC_AUTH_REALM_FILE:-/etc/elastisys/security/security-realm.properties}
# cert authentication
REQUIRE_CERT_AUTH=${REQUIRE_CERT_AUTH:-false}
CERT_AUTH_TRUSTSTORE=${CERT_AUTH_TRUSTSTORE:-/etc/elastisys/security/server_truststore.jks}
CERT_AUTH_TRUSTSTORE_PASSWORD=${CERT_AUTH_TRUSTSTORE_PASSWORD:-truststorepassword}


SECURITY_OPTS="--ssl-keystore ${SSL_KEYSTORE} --ssl-keystore-password ${SSL_KEYSTORE_PASSWORD}"
# require clients to do basic authentication against a security realm
if ${REQUIRE_BASIC_AUTH} ; then
    SECURITY_OPTS="${SECURITY_OPTS} --require-basicauth --require-role ${BASIC_AUTH_ROLE} --realm-file ${BASIC_AUTH_REALM_FILE}"
fi
# require clients to authenticate with a trusted certificate
if ${REQUIRE_CERT_AUTH} ; then
    SECURITY_OPTS="${SECURITY_OPTS} --require-cert -ssl-truststore ${CERT_AUTH_TRUSTSTORE} --ssl-truststore-password ${CERT_AUTH_TRUSTSTORE_PASSWORD}"
fi


SERVER_OPTS="--storage-dir ${STORAGE_DIR} --https-port ${HTTPS_PORT} ${SECURITY_OPTS}"

# Java system properties
JVM_OPTS=${JVM_OPTS:--Xmx128m}
JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=${JUL_CONFIG} -Dlogback.configurationFile=${LOG_CONFIG}"
JAVA_OPTS="${JAVA_OPTS} -DLOG_DIR=${LOG_DIR}"

# to allow use of ports below 1024, use: authbind --deep <prog> [args ...]
# note: assumes that /etc/authbind/byport/<port> files with proper owner/mode
# have been set up
authbind --deep ${java} ${JVM_OPTS} ${JAVA_OPTS} -jar awsaspool.jar ${SERVER_OPTS}
