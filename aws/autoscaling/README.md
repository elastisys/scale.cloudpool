# AWS AutoScaling cloud pool

The [elastisys](http://elastisys.com/) AWS AutoScaling 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages an AWS Auto Scaling Group. The size of the AWS Auto Scaling Group
is adjusted over the AWS Auto Scaling API, according to the desired size 
that the cloud pool has been instructed to maintain.

This cloud pool assumes that an AWS *Auto Scaling group* with a proper
*launch configuration* (specifying how new instances are to be created)
has already been created by external means (for instance, through the AWS
Auto Scaling command-line interface). Load-balancing between instances, if
needed, is also assumed to be taken care of, for example, via Elastic Load
Balancer or with a custom-made load balancing solution. It should be noted
that the AWS AutoScaling cloud pool can manage a pool of either on-demand
instances or spot instances, it all depends on what kind of instances you
have chosen to work with in your launch configuration.


The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API 
reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).

*Note that if you plan on using a large number of instances for your service, you
may need to contact Amazon to have them raise the appropriate 
[usage limits](http://docs.aws.amazon.com/general/latest/gr/aws_service_limits.html) for your account.
To submit a limit increase request, go to [AWS Support Center](https://console.aws.amazon.com/support/home#/)
and complete the request form.*



## Configuration
The `awsaspool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings` 
and `provisioningTemplate`), the `awsaspool` requires input similar
to the following:

```javascript
    ...	
    "cloudApiSettings": {
        "awsAccessKeyId": "ABC...123",
        "awsSecretAccessKey": "DEF...456",
        "region": "eu-west-1",
        "connectionTimeout": 10000,
        "socketTimeout": 10000
    },
    
    "provisioningTemplate": {
        "autoScalingGroup": "WebServers"
    },
	...
```



The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `awsAccessKeyId`: Your [AWS Access Key ID](https://aws-portal.amazon.com/gp/aws/securityCredentials). 
    - `awsSecretAccessKey`: Your [AWS Secret Access Key](https://aws-portal.amazon.com/gp/aws/securityCredentials). 
    - `region`: The [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) to connect to.
    - `connectionTimeout`: The timeout in milliseconds until a connection is established.
    - `socketTimeout`: The socket timeout (`SO_TIMEOUT`) in milliseconds, which is the
	  timeout for waiting for data or, put differently, a maximum period inactivity between 
	  two consecutive data packets.
		
- `provisioningTemplate`: Describes how to provision additional servers (on scale-up).
    - `autoScalingGroup`: The name of an existing Auto Scaling Group whose size is 
	  to be managed. May be left out. Default: the `name` set for the cloud pool.



## Usage
This module produces an executable jar file for the cloud pool server.
The simplest way of starting the server is to run

    java -jar <jar-file> --http-port=8080

which will start a server listening on HTTP port `8080`. 

*Note: for production settings, it is recommended to run the server with an HTTPS port.*

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
The module's build file contains a build goal that can be used to produce a
Docker image, once the project binary has been built in the `target`
directory (for example, via `mvn package`). Whenever `mvn install` is
executed the Docker image gets built locally on the machine. When
`mvn deploy` is run, such as during a release, the image also gets
pushed to our private docker registry.

*Note: make sure that you have issued `docker login` against the docker registry before trying to push an image.*



### Running a container from the image
Once the docker image is built for the server, it can be run by either 
specfying a HTTP port or an HTTPS port. For example, running with an HTTP port:

    docker run -d -p 8080:80 -e HTTP_PORT=80 <image>

This will start publish the container's HTTP port on host port `8080`.

*Note: for production settings, it is recommended to run the server with an HTTPS port.*

The following environment variables can be passed to the Docker container (`-e`)
to control its behavior. At least one of `${HTTP_PORT}` and `${HTTPS_PORT}` 
_must_ be specified.


Singleton/multipool mode:

  - `MULTIPOOL`: Set to `true` to start the server in [multipool](../../multipool/README.md)-mode, 
    in which it will publish a dynamic collection of *cloudpool instances*.
    The default is to run the server as a singleton cloudpool.

HTTP/HTTPS configuration:

  - `HTTP_PORT`: Enables a HTTP port on the server.  

  - `HTTPS_PORT`: Enables a HTTPS port on the server.  
    *Note: when specified, a `${SSL_KEYSTORE}` must be specified to identify to clients.*
	
  - `SSL_KEYSTORE`: The location of the server's SSL key store (PKCS12 format).  
     You typically combine this with mounting a volume that holds the key store.  
	 *Note: when specified, an `${SSL_KEYSTORE_PASSWORD}` must is required.*
	 
  - `SSL_KEYSTORE_PASSWORD`: The password that protects the key store.  

Runtime configuration:

  - `STORAGE_DIR`: destination folder for runtime state.  
    *Note: to persist across container recreation, this directory should be 
	mapped via a volume to a directory on the host.*  
    Default: `/var/lib/elastisys/awsaspool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/awsaspool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/awsaspool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/awsaspool`.

Client authentication:

  - `REQUIRE_BASIC_AUTH`: If `true`, require clients to provide username/password
    credentials according to the HTTP BASIC authentication scheme.  
    *Note: when specified, `${BASIC_AUTH_REALM_FILE}` and `${BASIC_AUTH_ROLE}` must be specified to identify trusted clients.*  
	Default: `false`.
  - `BASIC_AUTH_ROLE`: The role that an authenticated user must be assigned to be granted access to the server.  
  - `BASIC_AUTH_REALM_FILE`: A credentials store with users, passwords, and
    roles according to the format prescribed by the [Jetty HashLoginService](http://www.eclipse.org/jetty/documentation/9.2.6.v20141205/configuring-security-authentication.html#configuring-login-service).  
  - `REQUIRE_CERT_AUTH`: Require SSL clients to authenticate with a certificate,
    which must be included in the server's trust store.  
    *Note: when specified, `${CERT_AUTH_TRUSTSTORE}` and `${CERT_AUTH_TRUSTSTORE_PASSWORD}` must be specified to identify trusted clients.*  	
  - `CERT_AUTH_TRUSTSTORE`. The location of a SSL trust store (JKS format), containing trusted client certificates.
  - `CERT_AUTH_TRUSTSTORE_PASSWORD`: The password that protects the SSL trust store.

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
authentication is enforced (`--require-cert`), one needs to pass client 
certificate credentials via `curl`: 

    --key-type pem --key key.pem --cert-type pem --cert cert.pem

Here are some examples illustrating basic interactions with the cloud pool
(these assume that the server was started on a HTTP port):


1. Retrieve the currently set configuration document:

        curl -X GET http://localhost:8080/config
    

2. Set configuration:

        curl --header "Content-Type:application/json" \
                    -X POST -d @myconfig.json http://localhost:8080/config


3. Start:

        curl -X POST http://localhost:8080/start


4. Retrieve the current machine pool:

        curl -X POST http://localhost:8080/pool

5. Request the machine pool to be resized to size ``4``:

        curl --header "Content-Type:application/json" \
                    -X POST -d '{"desiredCapacity": 4}' http://localhost:8080/config
