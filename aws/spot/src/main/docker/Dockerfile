FROM java:openjdk-8-jdk
MAINTAINER Elastisys <techteam@elastisys.com>

# install spotpool server
COPY ${project.build.finalName}.jar /opt/elastisys/spotpool/spotpool.jar
COPY opt/elastisys/spotpool/start.sh /opt/elastisys/spotpool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/spotpool/start.sh


EXPOSE 443
CMD ["/opt/elastisys/spotpool/start.sh"]