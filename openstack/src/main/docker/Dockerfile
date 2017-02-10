FROM docker.elastisys.com:5000/elastisys/openjdk-public-dist:1.0
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install openstackpool server
COPY cloudpool.openstack.jar /opt/elastisys/openstackpool/openstackpool.jar
COPY start.sh /opt/elastisys/openstackpool/start.sh

ADD third-party-licenses /usr/share/doc/elastisys/third-party-licenses

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chmod +x /opt/elastisys/openstackpool/start.sh

CMD ["/opt/elastisys/openstackpool/start.sh"]
