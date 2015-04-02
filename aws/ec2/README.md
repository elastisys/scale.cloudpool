# AWS EC2 cloud pool
The [elastisys:scale](http://elastisys.com/scale) AWS EC2 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of EC2 instances. Pool members are identified by a 
configurable tag and instances are continuously provisioned/decommissioned to 
keep the pool's actual size in sync with the desired size that the cloud 
pool has been instructed to maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys:scale](http://elastisys.com/scale) cloud pool, through which
a client (for example, an autoscaler) can manage the pool.
For the complete API reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).



## Configuration
The `ec2pool` is configured with a JSON document such as the following:

```javascript
{
  {
    "cloudPool": {
      "name": "MyScalingPool",
      "driverConfig": {
        "awsAccessKeyId": "AXZ31...Q",
        "awsSecretAccessKey": "afAC/3Dd...s",
        "region": "eu-west-1"
      }
    },
    "scaleOutConfig": {
      "size": "m1.small",
      "image": "ami-018c9568",
      "keyPair": "instancekey",
      "securityGroups": ["webserver"],
      "bootScript": [
        "#!/bin/bash",
        "sudo apt-get update -qy",
        "sudo apt-get install -qy apache2"
      ]
    },
    "scaleInConfig": {
      "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
      "instanceHourMargin": 300
    },
    "alerts": {
      "smtp": [
        {
          "subject": "[elastisys:scale] cloud pool alert for MyScalingPool",
          "recipients": ["receiver@destination.com"],
          "sender": "noreply@elastisys.com",
          "smtpClientConfig": {
            "smtpHost": "mail.server.com",
            "smtpPort": 465,
            "authentication": {"userName": "john", "password": "secret"},
            "useSsl": True
          }
        }
      ],
      "http": [
        {
          "destinationUrls": ["https://some.host1:443/"],
          "severityFilter": "ERROR|FATAL",
          "auth": {
            "basicCredentials": { "username": "user1", "password": "secret1" }
          }
        },
        {       
          "destinationUrls": ["https://some.host2:443/"],
          "severityFilter": "INFO|WARN", 
          "auth": {
            "certificateCredentials": { "keystorePath": "src/test/resources/security/client_keystore.p12", "keystorePassword": "secret" }
          }
        }        
      ]
    },
    "poolUpdatePeriod": 120
  }
}
```

The configuration keys have the following meaning:

  - ``cloudPool``: Describes how to identify/manage pool members 
    and connect to the cloud provider.
    - ``name``: The logical name of the managed machine pool. All EC2 instances with this 
      [tag](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Using_Tags.html)
      are to be considered members of the pool.
    - ``awsAccessKeyId``: Your [AWS Access Key ID](https://aws-portal.amazon.com/gp/aws/securityCredentials). 
    - ``awsSecretAccessKey``: Your [AWS Secret Access Key](https://aws-portal.amazon.com/gp/aws/securityCredentials). 
    - ``region``: The [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) to connect to.
  - ``scaleOutConfig``: Describes how to provision additional servers (on scale-up).
    - ``size``: The [instance type](http://aws.amazon.com/ec2/instance-types/instance-details/)
      to use for new machine instances.
    - ``image``: The [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html)
      id to use for new machine instances.
    - ``keyPair``: The AWS EC2 [key pair](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)
      used to launch new machine instances.
    - ``securityGroups``: The list of [security groups](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-network-security.html)
      to apply to a new instance.
    - ``bootScript``: The script to run after first boot of a new instance.
  - ``scaleInConfig``: Describes how to decommission servers (on scale-down).
    - ``victimSelectionPolicy``: Policy for selecting which instance to 
      terminate. Allowed values: ``NEWEST_INSTANCE``, ``OLDEST_INSTANCE``, 
      ``CLOSEST_TO_INSTANCE_HOUR``.
    - ``instanceHourMargin``: How many seconds prior to the next instance hour 
      an acquired machine instance should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-down is ordered.
  - ``alerts``: Configuration that describes how to send alerts via email or HTTP(S) webhooks.
    - ``smtp``: a list of email alert senders
      - ``subject``: The subject line to use in sent mails (Subject).
      - ``recipients``: The receiver list (a list of recipient email addresses).
      - ``sender``: The sender email address to use in sent mails (From).
      - ``severityFilter``: A regular expression used to filter alerts. Alerts 
        with a severity (one of ``DEBUG``, ``INFO``, ``NOTICE``, ``WARN``, 
        ``ERROR``, ``FATAL``) that doesn't match the filter expression are 
        suppressed and not sent. Default: ``.*``.
        - ``smtpClientConfig``: Connection settings for the SMTP client.
          - ``smtpHost``: SMTP server host name/IP address.
          - ``smtpPort``: SMTP server port. Default is 25.
          - ``authentication``: Optional username/password to authenticate with SMTP
            server. If left out, authentication is disabled.
          - ``useSsl``: Enables/disables the use of SSL for SMTP connections. Default 
            is false (disabled).
    - ``http``: a list of HTTP(S) webhook alert senders, which will ``POST`` alerts
       to the specified endpoint using the (optional) configured authentication 
       credentials.
      - ``destinationUrls``: The list of destination endpoint URLs.
      - ``severityFilter``: A regular expression used to filter alerts. Alerts 
        with a severity (one of ``DEBUG``, ``INFO``, ``NOTICE``, ``WARN``, 
        ``ERROR``, ``FATAL``) that doesn't match the filter expression are 
        suppressed and not sent. Default: ``.*``.
      - ``auth``: Authentication credentials. Can specify either ``basicCredentials``
        or ``certificateCredentials`` or both.
        - ``basicCredentials``: ``username`` and ``password`` to use for BASIC-style
          authentication.
        - ``certificateCredentials``: ``keystorePath`` and ``keystorePassword``
          for client certificate-based authentication.
  - ``poolUpdatePeriod`` (optional): The time interval (in seconds) between 
    periodical pool size updates. A pool size update may involve terminating 
    termination-due instances and placing new instance requests to replace 
    terminated instances. Default: 60.


## Usage
This module produces an executable jar file for the cloud pool server.
The simplest way of starting the server is to run

    java -jar <jar-file>

which will start the server on HTTPS port ``8443``. 

*Note: this will run the server on a built-in, self-signed SSL certificate, which is not recommended for production settings.*

For a complete list of options, including the available security options,
run the server with the ``--help`` flag:

    java -jar <jar-file> --help



## Running the cloud pool in a Docker container
The cloud pool can be executed inside a 
[Docker](https://www.docker.com/) container. First, however, a docker image 
needs to be built that includes the cloud pool. The steps for building 
the image and running a container from the image are outlined below.

Before proceeding, make sure that your user is a member of the `docker` user group.
Without being a member of that user group, you won't be able to use docker without
 sudo/root privileges. 

See the [docker documentation](https://docs.docker.com/installation/ubuntulinux/#giving-non-root-access) 
for more details.



### Building the docker image
A `Dockerfile` is included under `src/main/docker` and can be used to generate 
the docker image. The module's build file also contains a build goal that can
be used to produce the image, once the project binary has been built in the `target`
directory (for example, via `mvn package`). To build the image simply run:

    mvn exec:exec -Dexec.executable=docker

To specify a different version for the image tag than the default (the version 
specified in the pom.xml), pass a `-Ddocker.image.version=<version>` option 
with the desired version name.

If you want to build the image yourself, issue the following commands:

    cd target/
    docker build --tag "elastisys/ec2pool:<version>" .



### Running a container from the image
Once the docker image is built for the cloud pool, it can be run with:

    docker run -d -p 8443:443 elastisys/ec2pool:<version>

This will publish the container's HTTPS port on host port 8443.



### Debugging a running container
The simplest way to debug a running container is to get a shell session via
  
    docker exec -it <container-id/name> /bin/bash

and check out the log files under `/var/log/elastisys`. Configurations are
located under `/etc/elastisys` and binaries under `/opt/elastisys`.



## Interacting with the cloud pool over its REST API
The following examples, all using the [curl](http://en.wikipedia.org/wiki/CURL) 
command-line tool, shows how to interact with the cloud pool over its
[REST API](http://cloudpoolrestapi.readthedocs.org/en/latest/).

The exact command-line arguments to pass to curl depends on the security
settings that the server was launched with. For example, if client-certificate
authentication is enforced (`--cert-required`), the `<authparams>` parameter 
below can be replaced with:

    --key-type pem --key credentials/client_private.pem \
    --cert-type pem --cert credentials/client_certificate.pem

Here are some examples illustrating basic interactions with the cloud pool endpoint:

 1. Retrieve configuration JSON schema (note: requires ``--config-handler`` to be turned on):

    ```
    curl -v --insecure <authparams> -X GET https://localhost:8443/config/schema
    ```

 2. Retrieve the currently set configuration document (note: requires ``--config-handler`` to be turned on):

    ```
    curl -v --insecure <authparams> -X GET https://localhost:8443/config
    ```

 3. Set configuration (note: requires ``--config-handler`` to be turned on).
    This example assumes that the configuration file is named ``config.json``:

    ```
    curl -v --insecure <authparams> \
         -X POST -d @tests/config.json  --header "Content-Type:application/json" \
         https://localhost:8443/config
    ```

 4. Retrieve the current machine pool:

    ```
    curl -v --insecure <authparams> -X GET https://localhost:8443/pool
    ```

 5. Request the machine pool to be resized to size ``4``:

    ```
    curl -v --insecure <authparams> \
         -X POST -d '{"desiredCapacity": 4}' --header "Content-Type:application/json" \
         https://localhost:8443/pool
    ```

