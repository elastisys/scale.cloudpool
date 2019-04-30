FROM elastisys/openjdk-public-dist:1.0
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install citycloudpool server
COPY cloudpool.citycloud.jar /opt/elastisys/citycloudpool/citycloudpool.jar
COPY start.sh /opt/elastisys/citycloudpool/start.sh

ADD third-party-licenses /usr/share/doc/elastisys/third-party-licenses

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/citycloudpool/start.sh

CMD ["/opt/elastisys/citycloudpool/start.sh"]
