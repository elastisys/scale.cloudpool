FROM docker.elastisys.com:5000/elastisys/openjdk-public-dist:1.0
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install vspherepool server
COPY cloudpool.vsphere.jar /opt/elastisys/vspherepool/vspherepool.jar
COPY start.sh /opt/elastisys/vspherepool/start.sh

ADD third-party-licenses /usr/share/doc/elastisys/third-party-licenses

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/vspherepool/start.sh

CMD ["/opt/elastisys/vspherepool/start.sh"]
