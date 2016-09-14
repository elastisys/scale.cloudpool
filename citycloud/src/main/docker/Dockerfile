FROM openjdk:8-jdk-alpine
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install citycloudpool server
COPY ${project.build.finalName}.jar /opt/elastisys/citycloudpool/citycloudpool.jar
COPY start.sh /opt/elastisys/citycloudpool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/citycloudpool/start.sh

EXPOSE 443
CMD ["/opt/elastisys/citycloudpool/start.sh"]