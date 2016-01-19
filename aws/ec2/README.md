# AWS EC2 cloud pool
The [elastisys](http://elastisys.com/) AWS EC2 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of EC2 instances. Pool members are identified by a 
configurable tag and instances are continuously provisioned/decommissioned to 
keep the pool's actual size in sync with the desired size that the cloud 
pool has been instructed to maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool.
For the complete API reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).

*Note that if you plan on using a large number of on-demand instances for your service, you
may need to contact Amazon to have them raise the [instance usage limit](http://aws.amazon.com/ec2/faqs/#How_many_instances_can_I_run_in_Amazon_EC2) for your account.
To submit a limit increase request, go to [AWS Support Center](https://console.aws.amazon.com/support/home#/case/create?issueType=service-limit-increase&limitType=service-code-ec2-instances) and complete the request form.*

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
        "region": "eu-west-1",
        "connectionTimeout": 10000,
        "socketTimeout": 10000
      }
    },
    "scaleOutConfig": {
      "size": "m1.small",
      "image": "ami-018c9568",
      "keyPair": "instancekey",
      "securityGroups": ["webserver"],
      "encodedUserData": "<base-64 encoded data>"
    },
    "scaleInConfig": {
      "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
      "instanceHourMargin": 300
    },
    "alerts": {
      "duplicateSuppression": { "time": 5, "unit": "minutes" },
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

    "poolFetch": {
      "retries": { 
         "maxRetries": 3, 
         "initialBackoffDelay": {"time": 3, "unit": "seconds"}
      },
      "refreshInterval": {"time": 30, "unit": "seconds"},
      "reachabilityTimeout": {"time": 5, "unit": "minutes"}
    },    
    "poolUpdate": {
      "updateInterval": {"time": 1, "unit": "minutes"}
    }
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
    - ``connectionTimeout``: The timeout in milliseconds until a connection is established.
    - ``socketTimeout``: The socket timeout (``SO_TIMEOUT``) in milliseconds, which is the
	  timeout for waiting for data or, put differently, a maximum period inactivity between 
	  two consecutive data packets.        
  - ``scaleOutConfig``: Describes how to provision additional servers (on scale-up).
    - ``size``: The [instance type](http://aws.amazon.com/ec2/instance-types/instance-details/)
      to use for new machine instances.
    - ``image``: The [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html)
      id to use for new machine instances.
    - ``keyPair``: The AWS EC2 [key pair](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)
      used to launch new machine instances.
    - ``securityGroups``: The list of [security groups](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-network-security.html)
      to apply to a new instance.
    - ``encodedUserData``: A [base64-encoded](http://tools.ietf.org/html/rfc4648)
      blob of data used to pass custom data to started machines typically in
      the form of a boot-up shell script or cloud-init parameters. Can, for
      instance, be produced via `cat bootscript.sh | base64 -w 0`.
      Refer to the [Amazon documentation](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html) for details.
  - ``scaleInConfig``: Describes how to decommission servers (on scale-in).
    - ``victimSelectionPolicy``: Policy for selecting which instance to 
      terminate. Allowed values: ``NEWEST_INSTANCE``, ``OLDEST_INSTANCE``, 
      ``CLOSEST_TO_INSTANCE_HOUR``.
    - ``instanceHourMargin``: How many seconds prior to the next instance hour 
      an acquired machine instance should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-down is ordered.
  - ``alerts``: Configuration that describes how to send alerts via email or HTTP(S) webhooks.
    - ``duplicateSuppression`` (optional): Duration of time to suppress
      duplicate alerts from being re-sent. Two alerts are considered equal if
      they share topic, message and metadata tags.  
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
  - ``poolFetch`` (optional): Controls how often to refresh the cloud 
    pool member list and for how long to mask cloud API errors. 
    Default: ``retries``: 3 retries with 3 second initial exponential back-off delay,
    ``refreshInterval``: 30 seconds, ``reachabilityTimeout``: 5 minutes.
    - ``retries``: Retry handling when fetching pool members from the cloud API fails.
      - ``maxRetries``: Maximum number of retries to make on each attempt to fetch pool 
        members.
      - ``initialBackoffDelay``: Initial delay to use in exponential back-off on retries. 
        May be zero, which results in no delay between retries.        
    - ``refreshInterval``: How often to refresh the cloud pool's view of the machine 
      pool members.
    - ``reachabilityTimeout``: How long to respond with cached machine pool observations
      before responding with a cloud reachability error. In other words, for how long should
      failures to fetch the machine pool be masked.              
  - ``poolUpdate`` (optional): Controls the behavior with respect to how often to 
    attempt to update the size of the machine pool to match the desired size.
    - ``updateInterval``: The time interval between  periodical pool size updates. 
      Default: 60 seconds.

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


A few environment variables can be passed to the Docker container (`-e`)
to control its behavior:

Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).
    Default: `/etc/elastisys/ec2pool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.
    Default: `/etc/elastisys/ec2pool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).
    Default: `/var/log/elastisys/ec2pool`.
  - `STORAGE_DIR`: destination folder for runtime state.
    Default: `/var/lib/elastisys/ec2pool`.

Security-related:

  - `HTTPS_PORT`: The HTTPS port on which the server's REST API can be reached.
    Default: `443`.
  - `SSL_KEYSTORE`: The key store that holds the server's SSL certificate. 
     You may wan to combine this with mounting a volume that holds the key
     store. Default: `/etc/elastisys/security/server_keystore.p12`.   
  - `SSL_KEYSTORE_PASSWORD`: The password used to protect the key store.
     Default: `serverpassword`.
  - `REQUIRE_BASIC_AUTH`: If `true`, require clients to provide
    username/password credentials according to the HTTP BASIC authentication
    scheme. Default: `false`.   
  - `BASIC_AUTH_ROLE`: The role that an authenticated user must be assigned to
     be granted access to the server (only relevant if `${REQUIRE_BASIC_AUTH}`
     is `true`).
     Default: `USER`.
  - `BASIC_AUTH_REALM_FILE`: A credentials store with users, passwords, and
    roles according to the format prescribed by the [Jetty HashLoginService](http://www.eclipse.org/jetty/documentation/9.2.6.v20141205/configuring-security-authentication.html#configuring-login-service) (only relevant if `${REQUIRE_BASIC_AUTH}` is `true`).
    Default: `/etc/elastisys/security/security-realm.properties`.
  - `REQUIRE_CERT_AUTH`: Require SSL clients to authenticate with a certificate,
    which must be included in the server's trust store.
    Default: `false`.
  - `CERT_AUTH_TRUSTSTORE`. The location of a SSL trust store (JKS format),
    containing trusted client certificates (only relevant if
    `${REQUIRE_CERT_AUTH}` is `true`).
    Default: `/etc/elastisys/security/server_truststore.jks`
  - `CERT_AUTH_TRUSTSTORE_PASSWORD`: The password that protects the SSL trust
    store (only relevant if `${REQUIRE_CERT_AUTH}` is `true`).
    Default: `trustpassword`.

JVM-related:

  - `JVM_OPTS`: JVM settings such as heap size. Default: `-Xmx128m`.




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

