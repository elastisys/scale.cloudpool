FROM openjdk:8-jdk-alpine
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install gcepool server
COPY cloudpool.gce.jar /opt/elastisys/gcepool/gcepool.jar
COPY start.sh /opt/elastisys/gcepool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chmod +x /opt/elastisys/gcepool/start.sh

CMD ["/opt/elastisys/gcepool/start.sh"]
