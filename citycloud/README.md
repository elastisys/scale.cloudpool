# CityCloud cloud pool

The [elastisys](http://elastisys.com/) CityCloud 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of [CityCloud](https://www.citycloud.com/) servers. 
As it makes use of CityCloud's OpenStack API, it is implemented as a 
very thin wrapper over the 
[OpenStack cloudpool](https://github.com/elastisys/scale.cloudpool/tree/master/openstack).
Refer to it for more detailed documentation.


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
    docker build --tag "elastisys/citycloudpool:<version>" .



### Running a container from the image
Once the docker image is built for the cloud pool, it can be run with:

    docker run -d -p 8443:443 elastisys/citycloudpool:<version>

This will publish the container's HTTPS port on host port 8443.


A few environment variables can be passed to the Docker container (`-e`)
to control its behavior:

Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).
    Default: `/etc/elastisys/citycloudpool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.
    Default: `/etc/elastisys/citycloudpool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).
    Default: `/var/log/elastisys/citycloudpool`.
  - `STORAGE_DIR`: destination folder for runtime state.
    Default: `/var/lib/elastisys/citycloudpool`.

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

