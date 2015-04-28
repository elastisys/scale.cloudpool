FROM dockerfile/java:openjdk-7-jre
MAINTAINER Elastisys <techteam@elastisys.com>

RUN apt-get update -y && apt-get install -y \
      authbind \
      supervisor


# create elastisys user and add to sudoers
RUN addgroup --system elastisys && \
    adduser  --system --ingroup elastisys \
             --shell=/bin/bash --disabled-password --disabled-login \
             elastisys && \
    usermod --append --groups sudo elastisys
# set password for elastisys user
RUN echo 'elastisys:secret' | chpasswd
      
# install awsaspool server
COPY ${project.build.finalName}.jar /opt/elastisys/awsaspool/awsaspool.jar
COPY opt/elastisys/awsaspool/start.sh /opt/elastisys/awsaspool/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \    
    chown -R elastisys:elastisys /var/log/elastisys /var/run/elastisys /var/lib/elastisys /opt/elastisys /etc/elastisys && \
    chmod +x /opt/elastisys/awsaspool/start.sh

# make sure https port (443) can be used by server via authbind
# this is needed for ports below 1024
RUN touch /etc/authbind/byport/443 && \
    chown -R elastisys:elastisys /etc/authbind/byport/443 && \
    chmod 500 /etc/authbind/byport/443

# prepare supervisord, which will take care of running the services
COPY etc/supervisor/conf.d/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mkdir -p /var/log/supervisor

EXPOSE 443
# run supervisord, which takes care of starting the services (awsaspool)
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
