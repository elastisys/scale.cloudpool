FROM java:openjdk-8-jdk
MAINTAINER Elastisys <techteam@elastisys.com>
   
# install awsaspool server
COPY ${project.build.finalName}.jar /opt/elastisys/awsaspool/awsaspool.jar
COPY start.sh /opt/elastisys/awsaspool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/awsaspool/start.sh

EXPOSE 443
CMD ["/opt/elastisys/awsaspool/start.sh"]
