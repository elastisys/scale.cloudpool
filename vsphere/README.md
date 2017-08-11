# Vsphere cloud pool

The [elastisys](http://elastisys.com/) Vsphere
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of Vsphere virtual machines. Pool members are identified by a
configurable tag and virtual machines are continuously provisioned/decommissioned to
keep the pool's actual size in sync with the desired size that the cloud
pool has been instructed to maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API
reference, the reader is referred to the
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).



## Configuration
The `vspherepool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings`
and `provisioningTemplate`), the Vsphere cloud pool requires input similar
to the following:

```javascript
    ...
    "cloudApiSettings": {
        "url": "http://172.16.0.1/sdk",
        "username": "username",
        "password": "password"
    },

    "provisioningTemplate": {
        "template": "template",
        "folder": "folder",
        "resourcePool": "pool"
    },
	...
```



The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `url`: Endpoint URL of Vcenter. For example http://172.16.0.1/sdk.
    - `username`: Vcenter account user.
    - `password`: Vcenter account password.

- `provisioningTemplate`: Describes how to provision additional servers (on scale-up).
    - `template`: The name of the template used to clone new virtual machines.
    - `folder`: The location into which cloned virtual machines will be stored.
    - `resourcePool`: The name of the resourcePool which cloned virtual machines will use.



## Vsphere 
In order to use the `vspherepool` it is important to be familiar with its terminology and abstractions.
In particular `template`, `folder` and `resourcePool` is used to perform the provisioning. 

In order to clone a virtual machine an existing image is necessary. 
An image which purpose is to act as the source for cloning operations is referred to as a `template`.
[See the vmware documentation for more details about templates](https://pubs.vmware.com/vsphere-51/index.jsp?topic=%2Fcom.vmware.vsphere.vm_admin.doc%2FGUID-F40130B0-0194-4A41-91FA-1A967721924B.html).

Vcenter manages its logical inventory using `folder` objects.
The `folder` used for provisioning is the name of the folder virtual machines cloned from the `template` will be stored in.
This `folder` needs to exist in Vcenters directory structure.
[See the vmware documentation for more details about folders](https://pubs.vmware.com/vsphere-51/index.jsp?topic=%2Fcom.vmware.vsphere.vcenterhost.doc%2FGUID-031BDB12-D3B2-4E2D-80E6-604F304B4D0C.html).

In order to manage computing resources Vcenter employs one or more `resourcePools`.
This represents some distribution of resources among the hosts and can be used to structure or prioritize virtual machine resources.
The provisioning template refers to the `resourcePool` cloned virtual machines will be associated with.
[See the vmware documentation for more details about resourcePools](https://pubs.vmware.com/vsphere-4-esx-vcenter/index.jsp?topic=/com.vmware.vsphere.resourcemanagement.doc_41/managing_resource_pools/c_why_use_resource_pools.html).

When fetching the machine pool from the REST API Vsphere has a unique representation of some properties.
The `region` of the machine refers to the `resourcePool` the machine belongs to.
The `machineSize` is of the format "cpu-{number of virtual cpus}-ram-{memory in MB}" or "unknown" if the virtual hardware is inaccessible (when the machine is starting for example). 

## Limitations
The following API methods are unsupported

- Set membership status
- Set service state
- Detach machine
- Attach machine



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
  - `STDOUT_LOG_LEVEL`: output level for logging to stdout (note: log output 
    that is written to file includes `DEBUG` level).
    Default: `INFO`.

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

        curl -X GET http://localhost:8080/pool

5. Request the machine pool to be resized to size ``4``:

        curl --header "Content-Type:application/json" \
                    -X POST -d '{"desiredSize": 4}' http://localhost:8080/pool/size
