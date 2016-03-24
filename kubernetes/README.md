# Description

The [elastisys](http://elastisys.com/) Kubernetes
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages the size of a Kubernetes replication controller . 
All replication controller pods are considered as pool members and
the size (scale) of the replication controller is continously updated to 
keep the pool's actual size in sync with the desired size that the cloud 
pool has been instructed to maintain. The Kubernetes cloud pool performs
all operations across the API exposed by the 
[Kubernetes apiserver](http://kubernetes.io/docs/api/)

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool (with a few exceptions, see below)
through which a client (for example, an autoscaler) can manage the pool.

For the complete API reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).


## Limitations
The Kubernetes cloud pool does not implement the full 
[cloud pool API](http://cloudpoolrestapi.readthedocs.org/en/latest/), but
the following subset of operations: [set configuration](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#set-configuration), [get configuration](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-configuration) [start](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#start), [stop](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#stop), [get status](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-status), [get machine pool](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-machine-pool), [get size](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-pool-size), [set desired size](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#set-desired-size).



## Configuration
The `kubernetespool` accesses the Kubernetes apiserver to perform its work.
When accessing the apiserver, the `kubernetespool` needs to authenticate.
There are a couple of ways to authenticate the client -- (1) via a JSON Web
Token or (2) via TLS certificate. Either approach can be used, but as
explained in the [Kubernetes documentation](http://kubernetes.io/docs/user-guide/accessing-the-cluster/#accessing-the-api-from-a-pod) the preferred approach when running the `kubernetespool` inside a Kubernetes pod is to make use of
approach (1), since a service account token will always be available on the
pod's file system.

Tokens and certificates/keys can be passed either as a file system path
(referring to the file system on which the `kubernetespool` is
running) or be passed as (base 64-encoded) values. Configuration variables
ending in {@code Path} are used to pass file system references.

So, when running the `kubernetespool` from a pod inside the targeted
Kubernetes cluster one would typically make use of configuration similar
to the following:

```javascript
{
    "apiServer": {
        "host": "kubernetes",
        "port": 443
    },
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    },
    "auth": {
        "clientTokenPath": "/var/run/secrets/kubernetes.io/serviceaccount/token",
        "serverCertPath": "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
     },
     "poolUpdate": {
         "updateInterval": { "time": 5, "unit": "seconds" }
     }
}
```

This will manage the given ReplicationController using the given auth token to 
authenticate against the specified Kubernetes apiserver.

When running the `kubernetespool` outside Kubernetes one could either use
token-based or certificate-based authentication. Configuring the pool to use a
certificate and key to authenticate can be done as follows:

```javascript
{
    "apiServer": {
        "host": "172.17.4.101",
        "port": 443
    },
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    },
    "auth": {
        "clientCertPath": "/home/kube/ssl/admin.pem",
        "clientKeyPath": "/home/kube/ssl/admin-key.pem",
        "serverCertPath": "/home/kube/ssl/ca.pem"
     },
     "poolUpdate": {
         "updateInterval": { "time": 5, "unit": "seconds" }
     }
}
```

This will use the cert and key at the given file system path where the
`kubernetespool` is running.

To pass the cert and key by value, one can send the PEM-encoded cert and
key base64-encoded to the `kubernetespool`:

For example, first get the base64-encoded content of your certs and key 
as follows: `cat admin-key.pem | base64 -w 0`. Once done, the config may look 
something like:

```javascript
{
    "apiServer": {
        "host": "172.17.4.101",
        "port": 443
    },
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    },
    "auth": {
        "clientCert": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM3ekNDQWRlZ0F3SUJBZ0lKQVBjQWZWbE0rdGk4TUEwR0NTcUdTSWIzRFFFQkN3VUFNQkl4RURBT0JnTlYKQkFNTUIydDFZbVV0WTJFd0hoY05NVFl3TXpJeU1UTXlNREE1V2hjTk1UY3dNekl5TVRNeU1EQTVXakFWTVJNdwpFUVlEVlFRRERBcHJkV0psTFdGa2JXbHVNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDCkFRRUFvV2xpZmY0dE5zb2hDWnVGajlrSDMySmFpVXpRbE02cFJiM0cwMW5KRWh1dnZPYld2d1R6UW9XNXN0Tm8KNE44K0pFMm96VFBqREN6cjRsN1hnZ0lhS3dOeFdwaGplVU04bW96NklRQjdMQTc4N2U1eXJOUkJvMTN6R0txZQp0bTNFWFAzK0NrODVvZWtTZTB4QkhKYjNrd1NudmVXL2s0eEJic0h0RXVtSmNQYk1URys5aHc5SzNGMGUyTDJwCmRWd0U5RzVZRCt5UE5nVjJGVE9OKzFjR3hNbW02bzF6Zk1Ec3lSL3QwN1JpT0xCOVVqYWxvdThDSVcxYnVBY2MKWGhadVYwaXdoaVdrSERxRnplSTBiaUVTNzdocEtSYWVjSVN3N1FsQlpjYjBjMjBQQk4zbUROS0RZYXRDZU1YaApuem1QU0oySGJ3Ny9DUm1sd0ViQXRpaWhhUUlEQVFBQm8wVXdRekFKQmdOVkhSTUVBakFBTUFzR0ExVWREd1FFCkF3SUY0REFwQmdOVkhSRUVJakFnZ2dwcmRXSmxjbTVsZEdWemdoSnJkV0psY201bGRHVnpMbVJsWm1GMWJIUXcKRFFZSktvWklodmNOQVFFTEJRQURnZ0VCQUlzWFZwVERxSS9qZEUzM2tvK1ZqY21Dd1gyNDNLR0s0V3ExNGd3dwpEYVFKNXZIZTl5TXk5bkpOSnBPMDRpSnhrczdiTlZvcEZVQjI3SFBuUjRsdExKNDJpV1FtdU8rcU5FUnd1azlMCk9IYUYxcGF6RW1UeGxmajNvZzNQRmppNHBESkc3NlVvRmdvTEw0VmJRRlMrSVRhNTR2VjRnaEVxMFVrUDk2ckUKOE1xaEZ1dXlUejlFcmdhcWVnSjJtc2xvR0N3WHAvdlBtR0tPRzdlRS9Yb2FUZzR6ckJiN1RQbEpXZ0ZUa1NzMwp6V2lsZFRlRzRrU3JGMFBHWDk5MUtxRklhNVNTN295UnJEN3VZc1plcTc5cVhWVW8vTEh6MStvNE9CWVlGNGl5Ck5MeEpaZ1pyUzIrQ1FEaFdQcElHMDMzNm9zaGJWcDhGOXpGYVBsbkxMUjFwLzNVPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==",
        "clientKey": "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBb1dsaWZmNHROc29oQ1p1Rmo5a0gzMkphaVV6UWxNNnBSYjNHMDFuSkVodXZ2T2JXCnZ3VHpRb1c1c3RObzROOCtKRTJvelRQakRDenI0bDdYZ2dJYUt3TnhXcGhqZVVNOG1vejZJUUI3TEE3ODdlNXkKck5SQm8xM3pHS3FldG0zRVhQMytDazg1b2VrU2UweEJISmIza3dTbnZlVy9rNHhCYnNIdEV1bUpjUGJNVEcrOQpodzlLM0YwZTJMMnBkVndFOUc1WUQreVBOZ1YyRlRPTisxY0d4TW1tNm8xemZNRHN5Ui90MDdSaU9MQjlVamFsCm91OENJVzFidUFjY1hoWnVWMGl3aGlXa0hEcUZ6ZUkwYmlFUzc3aHBLUmFlY0lTdzdRbEJaY2IwYzIwUEJOM20KRE5LRFlhdENlTVhobnptUFNKMkhidzcvQ1JtbHdFYkF0aWloYVFJREFRQUJBb0lCQUhUandIUEZjakRQU0FXUgpIclFCVTNZdDM2cTJlZ2FKY29RUzNyMkhzOWp0TytMc3VHODB3b1ZXR2hpcWlMVHdkaXdNSVVZWllUOGIrT2JDCkVBY1NScWtIb1RzZVNFczBxZHF5WlNFcEhBbllBTXE5ZDBZNW9COFNsazB5b2lVeWNKVjNTbFZrOGpPU2VkUFkKY1A2blJUcXVrRnN3MmYrYi9uYWE4WGhVcnplaUc2QVVXdXlsKzhUOCtGSThYTlRLdXJ6ZUJDbWRLbGZYZDhsYQp3cGhRRjVkZHJVbGFqNndEVEtzSmxYWDFEbHFKckI2VWZXVTFlYTV6OVNDelU0MjBaczhYZjJ4UENrZXBwa1o5CkFxdWdEWDEvMXdsNHdFOTNXOUp4V09LMFlWQmRGZGZ0dU9HYWpGRmcxM3BwUlpKbDdKYnNVbUkzUlBZQ09HSXcKTUxLM050RUNnWUVBMWFzUThvUnBhSHhWa08rdGJUQ0xGNFlGSVRrQWg5LytUQU9XN0VMWjZNVTlPTnlWWFNaeApzSHVPakdDb0loay9zV3NxYkMxS0JUOEVIamdYSDYwWkI3czhHZnZtZDNmckh6SUNDNC9iSjFyRkFFeHAyVU51CncxdXdQazBTS0FNWk9QN0lOYU11UzlwemhoUTFmejQxM0VsQVZCTnlPUG1KanVKaHJFZ3B1RmNDZ1lFQXdXUHgKZm5tQmNLYzUvZnNUWS92dlBvODVYK0VUd0J1bnE3Ri9rYXhPS3czL1o3enVJcG1jWGNiRkk1SmMyMkxvcUtWWQphTi9rSUJqUkxLVXlaWmF3V1pTU1NBYVpFV0J1cDNHNFIrUW1FV1YwYVBaN0lycVB5eDlsOEpWKzBLOXNyZ1FxCnN5Z05waFdYaDIxNGl2Y2ZNVXlaK1N6VG1sYytNZnhWNkRSalhEOENnWUVBdG9vSDEzaGg2UjdYcHhQc0VLMTUKRnVhck9UL09nVVpPcFRnbjFyNGlGaWR6YjBHYjVWR3pyUGRSeUFISGdpSVo5UU85NFY4cnJxR3diZlN6Wko5bwpFOS9VcjhveGtYMEVoTWtmVUN0ZEtoajAxcFZ4bEdoMGx6ZWNzUXo4NXV3R3YxZURTYmVZRkx1VEdFZnBrRVJnCmxVcUxSNGk1ZTQxTUJLTEltUHVwa004Q2dZQjNISVNJUG5SQUcyOTNoQ1lNUmdhMEJHajFLZDhOU3JzNTM2aFAKNDgxOWJUQ3JCMDJ3MStYY1NHbnhuOXM3Y0s4VitFajh4ekZ0cDN0bVFSVktSc2ExVmZISEZQRkFKNkhmMWdZSAptWGpzN0EwSC9SQVljc25QOUxYSHVYd1RNb2tBb1NaZmxFTGIwWjZ6MWZRUnUyVmw2dVZHK0pvWURMWU0rWHM3Cit0QmI1d0tCZ0FGZWhIcG1WMG4xK3cwc2sxRDFzREZ1U3Z2WmlBMGlIVzFoMlVURVgyTE54RDcrQ0xzZFBvdzMKTnQ1RkM3RURzVVNiblpNblRzMlpMZndlSEg5a0k4VFFvL2NFcEE2TVcxaEc2MmJrYlA4Z0lDaWQrUE56TEZqdQpQckRZMHZITW9ERGR0WkFzKzNrbHh2UTZtK1JkWEhySHMzamQyTmhlSFpzVUR3SnFTRnIzCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg==",
        "serverCert": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM5ekNDQWQrZ0F3SUJBZ0lKQVBlcDlWMzREVUxZTUEwR0NTcUdTSWIzRFFFQkN3VUFNQkl4RURBT0JnTlYKQkFNTUIydDFZbVV0WTJFd0hoY05NVFl3TXpJeU1UTXlNREE1V2hjTk5ETXdPREE0TVRNeU1EQTVXakFTTVJBdwpEZ1lEVlFRRERBZHJkV0psTFdOaE1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBCjBMVmdOV0JYN3JQS1VzY3I4blNDcjQ5QWU5aEtVc1FKREs5WEdlSytDdVVySC9KNDBXaWljaUg2K3g4R1kwaEQKdlJqOUZsL0xhNEdZT3NES0p2amtOWUhIb0l2Y1pXWGlWdXY4N3ExRWZvejZPYjAvUFRQMW14M2JLemRMVTVIeApvSDN1UjZBdkZPT3UyWHRLamRYSzhobjVUT1d2dXFmQU9iM3VhSVN6TXJsUGl3UFdoNTZjMU5wSjhXODFCY0tSCklBa2JmSmRSQkxaakk5eUp5ajNIaXRZWThOazI2SjhZQ3dZaGhHYmtNdGszSmZXcDdkbjgwWENiak44WGxIT2MKM01tM0xGNGFYSVVtcGxDSEh2elZoaTF6aHJyZnhQMGZ2cnBuaFhLNlBZT1pQbE5lMVRFeGxqOHI0OTdIMmJtMQpiQzJhd2JONjhSeW5XMHY1ay9YWkJRSURBUUFCbzFBd1RqQWRCZ05WSFE0RUZnUVVwRFV3VUlWVHpLZGJEdmhsCk0wRHdUaHMxUnZrd0h3WURWUjBqQkJnd0ZvQVVwRFV3VUlWVHpLZGJEdmhsTTBEd1RoczFSdmt3REFZRFZSMFQKQkFVd0F3RUIvekFOQmdrcWhraUc5dzBCQVFzRkFBT0NBUUVBSkFtVzM1cjFQRU1MdUsyaVhWZGczOE9KNmQ3MAppRFBmRFdDemdLcEFneWFOV0ZGSStHUUxGRFJnSHV4L095Z1pkZEZFTVBNMVluRnl0ODFCb0JLMkRzYy9JUUJEClJsYzJKM21MTTFOdGtIY2JXc0liZU5VUmNwME1LbVFYeGZNemV5S1pwMmpaOTZvc0svWlM2V3ExQjhueFFqSU8KNGF1RGtaQjk2c202UExTZ2lwQytoU0FJTEFHWndPdTBOMHVJaUR5SUJyb2lDdEpLdVFoUitrRWNZc1BIYWhUVAo0TFVSeWowUW4rRVVZTUJSb3A5RXJUYit1RFZ3SEZYbDB3STFXbWlhanB1czlSeml4ZmdCOEx1MjdVMkRnUWdNCkZlNWZjYVdrNU1ndzJEc1hXOUNEZVorQkxpelJQbmtocVNIRm53SjFNQ3hpYWc4TjRxRHNqeTNqRmc9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg=="
     },
     "poolUpdate": {
         "updateInterval": { "time": 5, "unit": "seconds" }
     }
}
```

A client token can also be passed "by-value" via the `clientToken` configuration
parameter.



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
    docker build --tag "elastisys/kubernetespool:<version>" .



### Running a container from the image
Once the docker image is built for the cloud pool, it can be run with:

    docker run -d -p 8443:443 elastisys/kubernetespool:<version>

This will publish the container's HTTPS port on host port 8443.

A few environment variables can be passed to the Docker container (`-e`)
to control its behavior:

Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).
    Default: `/etc/elastisys/kubernetespool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.
    Default: `/etc/elastisys/kubernetespool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).
    Default: `/var/log/elastisys/kubernetespool`.
  - `STORAGE_DIR`: destination folder for runtime state.
    Default: `/var/lib/elastisys/kubernetespool`.

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
