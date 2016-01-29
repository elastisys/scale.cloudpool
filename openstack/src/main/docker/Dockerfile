FROM java:openjdk-8-jdk
MAINTAINER Elastisys <techteam@elastisys.com>

# install openstackpool server
COPY ${project.build.finalName}.jar /opt/elastisys/openstackpool/openstackpool.jar
COPY opt/elastisys/openstackpool/start.sh /opt/elastisys/openstackpool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chmod +x /opt/elastisys/openstackpool/start.sh

EXPOSE 443
CMD ["/opt/elastisys/openstackpool/start.sh"]