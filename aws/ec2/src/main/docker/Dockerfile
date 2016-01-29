FROM java:openjdk-8-jdk
MAINTAINER Elastisys <techteam@elastisys.com>
    
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

EXPOSE 443
CMD ["/opt/elastisys/ec2pool/start.sh"]