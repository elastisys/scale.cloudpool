FROM openjdk:8-jdk-alpine
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install gkepool server
COPY cloudpool.google.container.jar /opt/elastisys/gkepool/gkepool.jar
COPY start.sh /opt/elastisys/gkepool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chmod +x /opt/elastisys/gkepool/start.sh

CMD ["/opt/elastisys/gkepool/start.sh"]
