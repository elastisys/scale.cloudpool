FROM openjdk:8-jdk-alpine
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install ec2pool server
COPY ${project.build.finalName}.jar /opt/elastisys/ec2pool/ec2pool.jar
COPY start.sh /opt/elastisys/ec2pool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/ec2pool/start.sh

CMD ["/opt/elastisys/ec2pool/start.sh"]
