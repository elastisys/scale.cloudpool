# OpenStack cloud pool

The [elastisys](http://elastisys.com/) OpenStack 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of OpenStack servers. Pool members are identified by a 
configurable tag and servers are continuously provisioned/decommissioned to 
keep the pool's actual size in sync with the desired size that the cloud 
pool has been instructed to maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API 
reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).



## Configuration
The `openstackpool` is configured with a JSON document such as the following:

```javascript
{
  {
    "cloudPool": {
      "name": "MyScalingPool",
      "driverConfig": {
        "auth": {
          "keystoneEndpoint": "http://keystone.host.com:5000/v2.0",
          "v2Credentials": {
            "tenantName": "tenant",
            "userName": "clouduser",
            "password": "cloudpass"          
          }
        },
        "region": "RegionOne",
        "assignFloatingIp": true
      }
    },
    "scaleOutConfig": {
      "size": "m1.small",
      "image": "ami-018c9568",
      "keyPair": "instancekey",
      "securityGroups": ["webserver"],
      "encodedUserData": "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg=="
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
    - ``name``: The logical name of the managed machine pool. All servers with this 
      tag are to be considered members of the pool.
      - ``auth``: Specifies how to authenticate against the OpenStack identity service (Keystone).
        - ``keystoneEndpoint``: Endpoint URL of the Keystone service. For example, http://172.16.0.1:5000/v2.0."
        - ``v2Credentials``: Credentials for using version 2 of the [identity HTTP API](http://docs.openstack.org/developer/keystone/http-api.html#history).
          See the section on authentication mechanisms below for other supported authentication schemes. 
          - ``tenantName``: OpenStack account tenant name.
          - ``userName``: OpenStack account user
          - ``password``: OpenStack account password
      - ``region``: The particular OpenStack region (out of the ones available in Keystone's 
        service catalog) to connect to. For example, ``RegionOne``.
      - ``assignFloatingIp``: Set to true if a floating IP address should be allocated 
        to launched servers. Default: ``true``.
  - ``scaleOutConfig``: Describes how to provision additional servers (on scale-up).
    - ``size``: The name of the server type to launch. For example, ``m1.medium``.
    - ``image``: The name of the machine image used to boot new servers.
    - ``keyPair``: The name of the key pair to use for new servers.
    - ``securityGroups``: The security group(s) to use for new servers.
    - ``encodedUserData``: A [base64-encoded](http://tools.ietf.org/html/rfc4648)
      blob of data used to pass custom data to started machines typically in the form 
      of a boot-up shell script or cloud-init parameters. Can, for instance, be produced 
      via `cat bootscript.sh | base64 -w 0`. Refer to the 
      [OpenStack documentation](http://docs.openstack.org/user-guide/cli_provide_user_data_to_instances.html) 
      for details.
  - ``scaleInConfig``: Describes how to decommission servers (on scale-down).
    - ``victimSelectionPolicy``: Policy for selecting which server to 
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
    termination-due servers and placing new server requests to replace 
    terminated servers. Default: 60.


## Supported Authentication Schemes
The ``openstackpool`` supports both version 2 and version 3 of the 
[identity HTTP API](http://docs.openstack.org/developer/keystone/http-api.html#history).
Configuring the pool for use of version 2 of the Keystone API is shown above.

Version 3 authentication supports either project-scoped or domain-scoped logins.
A configuration excerpt for a project-scoped authentication is shown here:

```javascript
     ...
     "auth": {  
        "keystoneUrl": "http://keystone.host.com:5000/v3/",  
        "v3Credentials": {
            "scope": {
               "projectId": "5acfdc77c79440bbbad61187999edd1f"
            },
            "userId": "fd33b32bc1234bc491947b3a88e67f71",
            "password": "secret"
        }
    },
    ...
```

Here, the ``projectId`` is the project identifier to which the user with id ``userId`` 
and password ``password`` belongs.

Similarly, for a domain-scoped login an ``auth`` configuration similar to the 
following can be used:

```javascript
     ...
     "auth": {  
        "keystoneUrl": "http://keystone.host.com:5000/v3/",  
        "v3Credentials": {
            "scope": {
               "domainId": "5dffdc77c79440bbbad61187999edd1f"
            },
            "userId": "fd33b32bc1234bc491947b3a88e67f71",
            "password": "secret"
        }
    },
    ...
```

With ``domainId`` being the domain identifier to which the user with id ``userId`` 
and password ``password`` belongs.


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
    docker build --tag "elastisys/openstackpool:<version>" .



### Running a container from the image
Once the docker image is built for the cloud pool, it can be run with:

    docker run -d -p 8443:443 elastisys/openstackpool:<version>

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

Here are some examples illustrating basic interactions with the cloud pool:

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

