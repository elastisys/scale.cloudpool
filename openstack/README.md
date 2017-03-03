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
The `openstackpool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings` 
and `provisioningTemplate`), the OpenStack cloud pool requires input similar
to the following:

```javascript
    ...	
    "cloudApiSettings": {
        "auth": {
            "keystoneEndpoint": "http://keystone.host.com:5000/v2.0",
            "v2Credentials": {
                "tenantName": "tenant",
                "userName": "clouduser",
                "password": "cloudpass"
            }
        },
        "region": "RegionOne",
        "connectionTimeout": 10000,
        "socketTimeout": 10000
    },
    
    "provisioningTemplate": {
        "size": "m1.small",
        "image": "Ubuntu  16.04.LTS",
        "keyPair": "my-ssh-loginkey",
        "securityGroups": ["webserver"],
        "encodedUserData": "IyEvYmluL2Jhc2gKCnN1ZG8gYXB0LWdldCB1cGRhdGUgLXF5CnN1ZG8gYXB0LWdldCBpbnN0YWxsIC1xeSBhcGFjaGUyCg=="
        "networks": ["private"],
        "assignFloatingIp": true,
    },
	...
```



The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `auth`: Specifies how to authenticate against the OpenStack identity service (Keystone).
        - `keystoneEndpoint`: Endpoint URL of the Keystone service. For example, http://172.16.0.1:5000/v2.0."
        - `v2Credentials`: Credentials for using version 2 of the [identity HTTP API](http://docs.openstack.org/developer/keystone/http-api.html#history).  
		  *Note: that the OpenStack cloud pool supports both Keystone version 2 and 3, see separate section below for details on the supported authentication schemes.*
          - `tenantName`: OpenStack account tenant name.
          - `userName`: OpenStack account user
          - `password`: OpenStack account password
      - `region`: The particular OpenStack region (out of the ones available in Keystone's 
        service catalog) to connect to. For example, `RegionOne`.
      - `connectionTimeout`: The timeout in milliseconds until a connection is established.
      - `socketTimeout`: The socket timeout (`SO_TIMEOUT`) in milliseconds, which is the
	    timeout for waiting for data or, put differently, a maximum period inactivity between 
	    two consecutive data packets.
		
- `provisioningTemplate`: Describes how to provision additional servers (on scale-up).
    - `size`: The name of the server type to launch.
    - `image`: The name of the machine image used to boot new servers.
    - `keyPair`: The name of the key pair to use for new servers. For example, `my-ssh-loginkey`.
    - `securityGroups`: The security group(s) to use for new servers.
    - `encodedUserData`: A [base64-encoded](http://tools.ietf.org/html/rfc4648)
      blob of data used to pass custom data to started machines typically in the form 
      of a boot-up shell script or cloud-init parameters. Can, for instance, be produced 
      via `cat bootscript.sh | base64 -w 0`. Refer to the 
      [OpenStack documentation](http://docs.openstack.org/user-guide/cli_provide_user_data_to_instances.html) 
      for details.
    - `networks`: The names of the networks to attach launched servers to (for example, `private`).
	  Each network creates a separate network interface controller (NIC) on a created server. Typically, 
	  this option can be left out, but in rare cases, when an account has more than one network to
      choose from, the OpenStack API forces us to be explicit about the network(s) we want to use.  
	  If left out, the default behavior is to use which ever network is configured by the cloud 
	  provider for the user/project. However, if there are multiple choices, this may cause 
	  server boot requests to fail.
    - `assignFloatingIp`: Set to `true` if a floating IP address should be allocated to launched servers. Default: `true`.



## Supported Authentication Schemes
The `openstackpool` supports both version 2 and version 3 of the 
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

Here, the `projectId` is the project identifier to which the user with id `userId` 
and password `password` belongs.

Similarly, for a domain-scoped login an `auth` configuration similar to the 
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

With `domainId` being the domain identifier to which the user with id `userId` 
and password `password` belongs.


## Usage
This module produces an executable jar file for the cloud pool server.
The simplest way of starting the server is to run

    java -jar <jar-file> --http-port=8080

which will start a server listening on HTTP port `8080`. 

*Note: for production settings, it is recommended to run the server with an HTTPS port.*

For a complete list of options, including the available security options,
run the server with the `--help` flag:

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

  - `MULTIPOOL`: Set to `true` to start the server in [multipool](../multipool/README.md)-mode, 
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
    Default: `/var/lib/elastisys/openstackpool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/openstackpool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/openstackpool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/openstackpool`.

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
