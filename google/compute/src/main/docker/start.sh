#!/bin/bash

set -e

# detect java location
JAVA_HOME="$(readlink -f /usr/bin/java | sed "s:/jre/bin/java::")"
java="${JAVA_HOME}/bin/java"

# Logging
LOG_CONFIG=${LOG_CONFIG:-/etc/elastisys/gcepool/logback.xml}
JUL_CONFIG=${JUL_CONFIG:-/etc/elastisys/gcepool/logging.properties}
# log destination dir for default LOG_CONFIG
LOG_DIR=${LOG_DIR:-/var/log/elastisys/gcepool}
mkdir -p ${LOG_DIR}
STDOUT_LOG_LEVEL=${STDOUT_LOG_LEVEL:-INFO}

#
# Runtime configuration
#

# where runtime state is kept
STORAGE_DIR=${STORAGE_DIR:-/var/lib/elastisys/gcepool}

RUNTIME_OPTS="--storage-dir ${STORAGE_DIR}"


#
# HTTP/HTTPS configuration
#

[[ -z "${HTTP_PORT}" && -z "${HTTPS_PORT}" ]] && echo "error: neither HTTP_PORT nor HTTPS_PORT specified" && exit 1

SERVER_OPTS=""
if [ "${HTTP_PORT}" != "" ]; then
    SERVER_OPTS="${SERVER_OPTS} --http-port=${HTTP_PORT}"
fi

# TLS/SSL settings
if [ "${HTTPS_PORT}" != "" ]; then
    SERVER_OPTS="${SERVER_OPTS} --https-port=${HTTPS_PORT}"
    [ -z "${SSL_KEYSTORE}" ] && echo "error: no SSL_KEYSTORE specified" && exit 1
    [ -z "${SSL_KEYSTORE_PASSWORD}" ] && echo "error: no SSL_KEYSTORE_PASSWORD specified" && exit 1
    SERVER_OPTS="${SERVER_OPTS} --ssl-keystore ${SSL_KEYSTORE} --ssl-keystore-password ${SSL_KEYSTORE_PASSWORD}"
fi



#
# Client authentication settings
#

# basic authentication
REQUIRE_BASIC_AUTH=${REQUIRE_BASIC_AUTH:-false}
# cert authentication
REQUIRE_CERT_AUTH=${REQUIRE_CERT_AUTH:-false}

AUTH_OPTS=""
# require clients to do basic authentication against a security realm
if ${REQUIRE_BASIC_AUTH} ; then
    [ -z "${BASIC_AUTH_REALM_FILE}" ] && echo "error: no BASIC_AUTH_REALM_FILE specified" && exit 1
    [ -z "${BASIC_AUTH_ROLE}" ] && echo "error: no BASIC_AUTH_ROLE specified" && exit 1
    AUTH_OPTS="${AUTH_OPTS} --require-basicauth --require-role ${BASIC_AUTH_ROLE} --realm-file ${BASIC_AUTH_REALM_FILE}"
fi
# require clients to authenticate with a trusted certificate
if ${REQUIRE_CERT_AUTH} ; then
    [ -z "${CERT_AUTH_TRUSTSTORE}" ] && echo "error: no CERT_AUTH_TRUSTSTORE specified" && exit 1
    [ -z "${CERT_AUTH_TRUSTSTORE_PASSWORD}" ] && echo "error: no CERT_AUTH_TRUSTSTORE_PASSWORD specified" && exit 1
    AUTH_OPTS="${AUTH_OPTS} --require-cert -ssl-truststore ${CERT_AUTH_TRUSTSTORE} --ssl-truststore-password ${CERT_AUTH_TRUSTSTORE_PASSWORD}"
fi


#
# Java system properties
#

JVM_OPTS=${JVM_OPTS:--Xmx128m}
JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=${JUL_CONFIG} -Dlogback.configurationFile=${LOG_CONFIG}"
# On SIGQUIT: make sure a thread dump written to ${LOG_DIR}/jvm.log
JAVA_OPTS="${JAVA_OPTS} -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=${LOG_DIR}/jvm.log -XX:-PrintConcurrentLocks"
# On OutOfMemory errors write a java_pid<pid>.hprof file to ${LOG_DIR}
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR} "
JAVA_OPTS="${JAVA_OPTS} -DLOG_DIR=${LOG_DIR}"


#
# Start
#

#
# If ${MULTIPOOL} is set to true, run as a multipool.
# Otherwise run it as a singleton cloudpool.
#
if [ "${MULTIPOOL}" = true ]; then
    ${java} ${JVM_OPTS} ${JAVA_OPTS} -cp /opt/elastisys/gcepool/gcepool.jar com.elastisys.scale.cloudpool.google.compute.server.multipool.Main ${RUNTIME_OPTS} ${SERVER_OPTS} ${AUTH_OPTS}
else
    ${java} ${JVM_OPTS} ${JAVA_OPTS} -jar /opt/elastisys/gcepool/gcepool.jar ${RUNTIME_OPTS} ${SERVER_OPTS} ${AUTH_OPTS}
fi
