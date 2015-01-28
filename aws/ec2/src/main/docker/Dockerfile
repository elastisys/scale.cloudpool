FROM dockerfile/java:openjdk-7-jre
MAINTAINER Elastisys <techteam@elastisys.com>

RUN apt-get update -y && apt-get install -y \
      authbind \
      supervisor \
      openssh-server


# create elastisys user and add to sudoers
RUN addgroup --system elastisys && \
    adduser  --system --ingroup elastisys \
             --shell=/bin/bash --disabled-password --disabled-login \
             elastisys && \
    usermod --append --groups sudo elastisys
      
# set login password
RUN echo 'elastisys:secret' | chpasswd

# install ec2adapter server
COPY ${project.build.finalName}.jar /opt/elastisys/ec2adapter/ec2adapter.jar
COPY opt/elastisys/ec2adapter/start.sh /opt/elastisys/ec2adapter/start.sh

# create log directory
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    chown -R elastisys:elastisys /var/log/elastisys /var/run/elastisys \
            /opt/elastisys && \
    chmod +x /opt/elastisys/ec2adapter/start.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# make sure https port (443) can be used by server via authbind
# this is needed for ports below 1024
RUN touch /etc/authbind/byport/443 && \
    chown -R elastisys:elastisys /etc/authbind/byport/443 && \
    chmod 500 /etc/authbind/byport/443

# prepare sshd
RUN mkdir -p /var/run/sshd

# prepare supervisord, which will take care of running the services
COPY etc/supervisor/conf.d/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mkdir -p /var/log/supervisor

EXPOSE 22 443
# run supervisord, which takes care of starting the services (ssh, ec2adapter)
CMD ["/usr/bin/supervisord"]