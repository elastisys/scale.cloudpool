# Description

The [elastisys](http://elastisys.com/) Kubernetes [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/), from hereon referred to as `kubernetespool`, manages the size of a group of pod replicas, either via a [ReplicationController](https://kubernetes.io/docs/user-guide/replication-controller/), or via a [ReplicaSet](https://kubernetes.io/docs/user-guide/replicasets/), or via a [Deployment](https://kubernetes.io/docs/user-guide/deployments/).

After setting up a ReplicationController, or a ReplicaSet, or a Deployment in Kubernetes via one of its [access methods](https://kubernetes.io/docs/user-guide/accessing-the-cluster/) (such as [kubectl](https://kubernetes.io/docs/user-guide/kubectl-overview/)), a `kubernetespool` can be told to manage the size of its pod replica group.

The number of pod replicas is continuously updated to keep the pool's actual size in sync with the desired size that the cloud pool has been instructed to maintain.

The `kubernetespool` supports the [cloudpool REST API](http://cloudpoolrestapi.readthedocs.org/en/latest/) with a few exceptions (see below).


## Limitations
The `kubernetespool` does not implement the full [cloud pool API](http://cloudpoolrestapi.readthedocs.org/en/latest/), but the following subset of operations: 

- [set configuration](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#set-configuration)
- [get configuration](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-configuration)
- [start](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#start)
- [stop](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#stop)
- [get status](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-status)
- [get machine pool](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-machine-pool)
- [get size](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#get-pool-size)
- [set desired size](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#set-desired-size)



## Configuration
The `kubernetespool` performs all operations against the 
[Kubernetes API server](http://kubernetes.io/docs/api/).

The `kubernetespool` expects a configuration document (either passed
on start-up via the `--config` command-line option or set
through the [set configuration](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html#set-configuration)
API method. The configuration specifies which Kubernetes API server
to connect to and how to authenticate against that server, as well
as the pod replica API construct to manage (either a `ReplicationController`,
or a `ReplicaSet`, or a `Deployment`).

A configuration document may look as follows. For more details on the 
available authentication options, refer to the [Authentication section](#authentication)
below.

```javascript
{
    "apiServerUrl": "https://kubernetes.host:443",

    "auth": {
        "clientCertPath": "/path/to/admin.pem",
        "clientKeyPath": "/path/to/admin-key.pem",
     },

    "podPool": {
        "namespace": "default",
        "replicationController": "nginx-rc"
     },
     
     "updateInterval": { "time": 15, "unit": "seconds" },

     "alerts": {
        ...
     }
}
```

The configuration keys carry the following semantics:

  - `kubeConfigPath`: A path to a kubeconfig file, from which Kubernetes client
    settings will be loaded. This option is mutually exclusive with
    `apiServerUrl` and `auth`. Note that the kubeconfig file must specify a
    `current-context`.

  - `apiServerUrl`: The base URL of the Kubernetes API server. This option is
    mutually exclusive with `kubeConfigPath`.

  - `auth`: API access credentials. This option is mutually exclusive with
    `kubeConfigPath`. Either certificate-based or token-based authentication can be
    used. Credentials can either be passed as file references or as
    base64-encoded values.  
    Refer to the [Authentication section](#authentication) for details.
    - Certificate-based client authentication (both cert and key must be specified):
      - `clientCert`: A base64-encoded PEM-encoded certificate.
      - `clientCertPath`: A path to a PEM-encoded certificate.
      - `clientKey`: A base64-encoded PEM-encoded key.
      - `clientKeyPath`: A path to a PEM-encoded key.
    - Token-based client authentication:
      - `clientToken`: A base64-encoded JSON Web Token.
      - `clientTokenPath`: A path to a base64-encoded JSON Web Token.
    - Server authentication (**optional**), if the API server's certificate
      is to be verified:
      - `serverCert`: A base64-encoded PEM-encoded server/CA certificate.
      - `serverCertPath`: A path to a PEM-encoded server/CA certificate.
  - `podPool` (**required**): Declares the API construct (`ReplicationController`,
    `ReplicaSet`, `Deployment`) whose pod set is to be managed. The configuration
    must specify one and only one of the `replicationController`, `replicaSet`, 
    `deployment` fields.
    - `namespace` (**required**): The [namespace](http://kubernetes.io/docs/user-guide/namespaces/)
      that the managed API construct exists in.
    - `replicationController`: The name of the `ReplicationController` to manage.
    - `replicaSet`: The name of the `ReplicaSet` to manage.
    - `deployment`: The name of the `Deployment` to manage.

  - `updateInterval` (*optional*): The delay between attempts to synchronize 
    the pool size with the desired size set with the `kubernetespool`.  
    Default: `10 seconds`.

  - `alerts` (*optional*): Configuration that describes how to send alerts 
    via email or HTTP(S) webhooks. These settings are described in great 
    detail in the [root-level README.md](../README.md#configuration)



### Authentication
To access the API server, the `kubernetespool` needs to be set up with 
appropriate authentication credentials. There are a couple of ways to 
authenticate the client -- (1) via a TLS certificate or (2) via a JSON 
Web Token. 

The first approach is the most common, especially when running the 
`kubernetespool` outside the Kubernetes cluster. When running the
`kubernetespool` as a pod inside the Kubernetes it is, however,
[recommended](http://kubernetes.io/docs/user-guide/accessing-the-cluster/#accessing-the-api-from-a-pod) 
to use approach (2), since a service account token will always be 
available on the pod's file system. However, whether it's a good 
idea to run the `kubernetespool` within the same Kubernetes cluster 
it is instructed to manage is arguable.

Tokens and certificates/keys can be passed either as file system paths
(referring to the file system on which the `kubernetespool` is
running) or be passed as (base 64-encoded) values. Configuration variables
ending in `Path` are used to pass file system references.



#### Running kubernetespool outside Kubernetes

The `kubernetespool` can be configured with a regular `kubectl`
[kubeconfig](https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/) 
file. The kubeconfig contains all connection details and auth credentials needed
to communicate with a certain cluster. Note that you need to have a
`current-context` set in the kubeconfig.

```javascript
{
    "kubeConfigPath": "/home/me/.kube/config",

    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    }
}
```

For those cases where no kubeconfig file is available, one can specify the
`apiServerUrl` and an `auth` field. One can either make use of token-based or
certificate-based authentication. Using cert-based authentication (which is
probably easier when running outside Kubernetes) could look something like:

```javascript
{
    "apiServerUrl": "https://some.host:443",
    "auth": {
        "clientCertPath": "/home/kube/ssl/admin.pem",
        "clientKeyPath": "/home/kube/ssl/admin-key.pem",
        "serverCertPath": "/home/kube/ssl/ca.pem"
    },
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    }
}
```

This will use the cert and key at the given file system locations where the
`kubernetespool` is running.


To pass the cert and key by value, one can send the PEM-encoded cert and
key base64-encoded to the `kubernetespool`:

For example, first get the base64-encoded content of your certs and key 
as follows: `cat admin-key.pem | base64 -w 0`. Once done, the config may look 
something like:

```javascript
{
    "apiServerUrl": "https://some.host:443",

    "auth": {
        "clientCert": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM3ekNDQWRlZ0F3SUJBZ0lKQVBjQWZWbE0rdGk4TUEwR0NTcUdTSWIzRFFFQkN3VUFNQkl4RURBT0JnTlYKQkFNTUIydDFZbVV0WTJFd0hoY05NVFl3TXpJeU1UTXlNREE1V2hjTk1UY3dNekl5TVRNeU1EQTVXakFWTVJNdwpFUVlEVlFRRERBcHJkV0psTFdGa2JXbHVNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDCkFRRUFvV2xpZmY0dE5zb2hDWnVGajlrSDMySmFpVXpRbE02cFJiM0cwMW5KRWh1dnZPYld2d1R6UW9XNXN0Tm8KNE44K0pFMm96VFBqREN6cjRsN1hnZ0lhS3dOeFdwaGplVU04bW96NklRQjdMQTc4N2U1eXJOUkJvMTN6R0txZQp0bTNFWFAzK0NrODVvZWtTZTB4QkhKYjNrd1NudmVXL2s0eEJic0h0RXVtSmNQYk1URys5aHc5SzNGMGUyTDJwCmRWd0U5RzVZRCt5UE5nVjJGVE9OKzFjR3hNbW02bzF6Zk1Ec3lSL3QwN1JpT0xCOVVqYWxvdThDSVcxYnVBY2MKWGhadVYwaXdoaVdrSERxRnplSTBiaUVTNzdocEtSYWVjSVN3N1FsQlpjYjBjMjBQQk4zbUROS0RZYXRDZU1YaApuem1QU0oySGJ3Ny9DUm1sd0ViQXRpaWhhUUlEQVFBQm8wVXdRekFKQmdOVkhSTUVBakFBTUFzR0ExVWREd1FFCkF3SUY0REFwQmdOVkhSRUVJakFnZ2dwcmRXSmxjbTVsZEdWemdoSnJkV0psY201bGRHVnpMbVJsWm1GMWJIUXcKRFFZSktvWklodmNOQVFFTEJRQURnZ0VCQUlzWFZwVERxSS9qZEUzM2tvK1ZqY21Dd1gyNDNLR0s0V3ExNGd3dwpEYVFKNXZIZTl5TXk5bkpOSnBPMDRpSnhrczdiTlZvcEZVQjI3SFBuUjRsdExKNDJpV1FtdU8rcU5FUnd1azlMCk9IYUYxcGF6RW1UeGxmajNvZzNQRmppNHBESkc3NlVvRmdvTEw0VmJRRlMrSVRhNTR2VjRnaEVxMFVrUDk2ckUKOE1xaEZ1dXlUejlFcmdhcWVnSjJtc2xvR0N3WHAvdlBtR0tPRzdlRS9Yb2FUZzR6ckJiN1RQbEpXZ0ZUa1NzMwp6V2lsZFRlRzRrU3JGMFBHWDk5MUtxRklhNVNTN295UnJEN3VZc1plcTc5cVhWVW8vTEh6MStvNE9CWVlGNGl5Ck5MeEpaZ1pyUzIrQ1FEaFdQcElHMDMzNm9zaGJWcDhGOXpGYVBsbkxMUjFwLzNVPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==",
        "clientKey": "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBb1dsaWZmNHROc29oQ1p1Rmo5a0gzMkphaVV6UWxNNnBSYjNHMDFuSkVodXZ2T2JXCnZ3VHpRb1c1c3RObzROOCtKRTJvelRQakRDenI0bDdYZ2dJYUt3TnhXcGhqZVVNOG1vejZJUUI3TEE3ODdlNXkKck5SQm8xM3pHS3FldG0zRVhQMytDazg1b2VrU2UweEJISmIza3dTbnZlVy9rNHhCYnNIdEV1bUpjUGJNVEcrOQpodzlLM0YwZTJMMnBkVndFOUc1WUQreVBOZ1YyRlRPTisxY0d4TW1tNm8xemZNRHN5Ui90MDdSaU9MQjlVamFsCm91OENJVzFidUFjY1hoWnVWMGl3aGlXa0hEcUZ6ZUkwYmlFUzc3aHBLUmFlY0lTdzdRbEJaY2IwYzIwUEJOM20KRE5LRFlhdENlTVhobnptUFNKMkhidzcvQ1JtbHdFYkF0aWloYVFJREFRQUJBb0lCQUhUandIUEZjakRQU0FXUgpIclFCVTNZdDM2cTJlZ2FKY29RUzNyMkhzOWp0TytMc3VHODB3b1ZXR2hpcWlMVHdkaXdNSVVZWllUOGIrT2JDCkVBY1NScWtIb1RzZVNFczBxZHF5WlNFcEhBbllBTXE5ZDBZNW9COFNsazB5b2lVeWNKVjNTbFZrOGpPU2VkUFkKY1A2blJUcXVrRnN3MmYrYi9uYWE4WGhVcnplaUc2QVVXdXlsKzhUOCtGSThYTlRLdXJ6ZUJDbWRLbGZYZDhsYQp3cGhRRjVkZHJVbGFqNndEVEtzSmxYWDFEbHFKckI2VWZXVTFlYTV6OVNDelU0MjBaczhYZjJ4UENrZXBwa1o5CkFxdWdEWDEvMXdsNHdFOTNXOUp4V09LMFlWQmRGZGZ0dU9HYWpGRmcxM3BwUlpKbDdKYnNVbUkzUlBZQ09HSXcKTUxLM050RUNnWUVBMWFzUThvUnBhSHhWa08rdGJUQ0xGNFlGSVRrQWg5LytUQU9XN0VMWjZNVTlPTnlWWFNaeApzSHVPakdDb0loay9zV3NxYkMxS0JUOEVIamdYSDYwWkI3czhHZnZtZDNmckh6SUNDNC9iSjFyRkFFeHAyVU51CncxdXdQazBTS0FNWk9QN0lOYU11UzlwemhoUTFmejQxM0VsQVZCTnlPUG1KanVKaHJFZ3B1RmNDZ1lFQXdXUHgKZm5tQmNLYzUvZnNUWS92dlBvODVYK0VUd0J1bnE3Ri9rYXhPS3czL1o3enVJcG1jWGNiRkk1SmMyMkxvcUtWWQphTi9rSUJqUkxLVXlaWmF3V1pTU1NBYVpFV0J1cDNHNFIrUW1FV1YwYVBaN0lycVB5eDlsOEpWKzBLOXNyZ1FxCnN5Z05waFdYaDIxNGl2Y2ZNVXlaK1N6VG1sYytNZnhWNkRSalhEOENnWUVBdG9vSDEzaGg2UjdYcHhQc0VLMTUKRnVhck9UL09nVVpPcFRnbjFyNGlGaWR6YjBHYjVWR3pyUGRSeUFISGdpSVo5UU85NFY4cnJxR3diZlN6Wko5bwpFOS9VcjhveGtYMEVoTWtmVUN0ZEtoajAxcFZ4bEdoMGx6ZWNzUXo4NXV3R3YxZURTYmVZRkx1VEdFZnBrRVJnCmxVcUxSNGk1ZTQxTUJLTEltUHVwa004Q2dZQjNISVNJUG5SQUcyOTNoQ1lNUmdhMEJHajFLZDhOU3JzNTM2aFAKNDgxOWJUQ3JCMDJ3MStYY1NHbnhuOXM3Y0s4VitFajh4ekZ0cDN0bVFSVktSc2ExVmZISEZQRkFKNkhmMWdZSAptWGpzN0EwSC9SQVljc25QOUxYSHVYd1RNb2tBb1NaZmxFTGIwWjZ6MWZRUnUyVmw2dVZHK0pvWURMWU0rWHM3Cit0QmI1d0tCZ0FGZWhIcG1WMG4xK3cwc2sxRDFzREZ1U3Z2WmlBMGlIVzFoMlVURVgyTE54RDcrQ0xzZFBvdzMKTnQ1RkM3RURzVVNiblpNblRzMlpMZndlSEg5a0k4VFFvL2NFcEE2TVcxaEc2MmJrYlA4Z0lDaWQrUE56TEZqdQpQckRZMHZITW9ERGR0WkFzKzNrbHh2UTZtK1JkWEhySHMzamQyTmhlSFpzVUR3SnFTRnIzCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg=="
    },
     
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    },
}
```



#### Running kubernetespool inside Kubernetes (as a pod)

When running the `kubernetespool` from a pod inside the targeted
Kubernetes cluster one would typically make use of configuration similar
to the one below. As explained in the 
[Kubernetes documentation](http://kubernetes.io/docs/user-guide/accessing-the-cluster/#accessing-the-api-from-a-pod),
a pod can always reach the API server via the `kubernetes` DNS entry and 
authentication credentials for the service user are available under 
`/var/run/secrets`):

```javascript
{
    "apiServerUrl": "https://kubernetes:443",
    "auth": {
        "clientTokenPath": "/var/run/secrets/kubernetes.io/serviceaccount/token",
        "serverCertPath": "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
    }
    "podPool": {
        "namespace": "default",
        "replicationController": "nginx"
    }
}
```

This will manage the given `ReplicationController` using the given auth token to 
authenticate against the specified Kubernetes apiserver.

A client token can also be passed "by-value" via the `clientToken` configuration
parameter.




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
    Default: `/var/lib/elastisys/kubernetespool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/kubernetespool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/kubernetespool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/kubernetespool`.
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

        curl -X POST http://localhost:8080/pool

5. Request the machine pool to be resized to size ``4``:

        curl --header "Content-Type:application/json" \
                    -X POST -d '{"desiredCapacity": 4}' http://localhost:8080/config
