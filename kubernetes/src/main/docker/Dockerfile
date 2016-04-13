FROM java:openjdk-8-jdk
MAINTAINER Elastisys <techteam@elastisys.com>

# install kubernetespool server
COPY ${project.build.finalName}.jar /opt/elastisys/kubernetespool/kubernetespool.jar
COPY start.sh /opt/elastisys/kubernetespool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chmod +x /opt/elastisys/kubernetespool/start.sh

EXPOSE 443
CMD ["/opt/elastisys/kubernetespool/start.sh"]