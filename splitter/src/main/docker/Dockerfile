#
# Note: an ssh daemon is installed, but the ${SSH_KEY} environment variable
#       needs to be set to the contents of a public key (such as ~/.ssh/id_rsa.pub)
#       and passed to docker at run-time in order to be able to log in as the
#       elastisys user. If set, the contents of ${SSH_KEY} will be set in
#       /home/elastisys/.ssh/authorized_keys. Therefore, to be able to log in
#       to the container you could, for example, run it as follows:
#
#           docker run -d -p 2222:22 -p 8443:443 \
#               -e "SSH_KEY=$(cat ~/.ssh/id_rsa.pub)" \
#               <image tag>
#
#        You will then be able to log in to the started container via:
#
#            ssh -i ~/.ssh/id_rsa -p 2222 elastisys@localhost
# 

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
      
# install splitteradapter server
COPY ${project.build.finalName}.jar /opt/elastisys/splitteradapter/splitteradapter.jar
COPY opt/elastisys/splitteradapter/start.sh /opt/elastisys/splitteradapter/start.sh
COPY opt/elastisys/seed-authorized_keys.sh /opt/elastisys/seed-authorized_keys.sh

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create log directory
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    chown -R elastisys:elastisys /var/log/elastisys /var/run/elastisys \
            /opt/elastisys && \
    chmod +x /opt/elastisys/splitteradapter/start.sh && \
    chmod +x /opt/elastisys/seed-authorized_keys.sh

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
# run supervisord, which takes care of starting the services (ssh, splitteradapter)
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]