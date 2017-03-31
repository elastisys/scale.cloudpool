# Google Container Engine (GKE) cloud pool

The [elastisys](http://elastisys.com/) Google Container Engine (GKE)
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/) 
manages the size of a Google Container Engine (GKE) container cluster.
The cloud pool modifies the size of the node pool(s) that the cluster 
is made up of to ensure that the actual cluster size is kept in sync 
with the desired size that the cloud pool has been instructed to maintain.
 



## Overview
To understand how the `gkepool` operates, some basic understanding of
GKE is required. For more details, refer to the 
[official GKE documentation](https://cloud.google.com/container-engine/docs/).

A container cluster consists of one [kubernetes master endpoint](https://cloud.google.com/container-engine/docs/clusters/#the_kubernetes_master), which hosts the API server and one or more 
[node instances](https://cloud.google.com/container-engine/docs/clusters/#nodes),
which are worker nodes onto which pods are scheduled by the master.

The `gkepool` only manages the number of node instances in the cluster, 
it never touches the master.

A cluster's node instances can be organized into one or more 
[node pools](https://cloud.google.com/container-engine/docs/node-pools).
A node pool consists of one or more 
[instance groups](https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups) 
(each being a group of machines sharing a common instance template). 
Each node pool typically consists of a single instance group but 
_may_ consist of several instance groups in different zones, in 
case the cluster is a [multi-zone cluster](https://cloud.google.com/container-engine/docs/multi-zone-clusters).

When the size of the managed cluster needs to change, _scaling policies_
can be used to determine which node pool (in case of a cluster with 
multiple node pools) and which instance group within that node pool 
whose target size is to be updated.

For the time being, only a single policy is supported:

  - `Balanced`: This policy strives to keep an equal number of nodes in 
    all node pools.  
    A new node is always added to the smallest instance group found in the 
	smallest node pool.  
    A node is always deleted from the largest instance group found in the
	largest node pool.

NOTE: it may take a few minutes for an added node to be fully launched 
and ready for use in the Kubernetes cluster 
(that is, it appears as `Ready` in `kubectl get nodes`).



## Prerequisites
The `gkepool` needs a Google Cloud Platform 
[service account](https://cloud.google.com/compute/docs/access/service-accounts) 
to authenticate its API calls to Google.

To set up a service account, follow these steps:


1. [Register for a Google Cloud Platform account](https://cloud.google.com/)

2. [Create a Google Cloud Platform project](https://cloud.google.com/resource-manager/docs/creating-project#via_console)

3. Enable the Google Compute Engine API for your project's [API Manager](https://console.cloud.google.com/apis/dashboard).

4. Set up a service account in your project's [IAM service](https://console.cloud.google.com/iam-admin/iam/), 
   which enables app-level authentication. Since the cloudpool needs to both read and 
   modify cloud resources, choose `Project Editor` as `Role`. You'll also need to create 
   and download a `.json` service account key, which is used to authorise your API calls 
   to Google.

5. [Create a container cluster](https://cloud.google.com/container-engine/docs/clusters/operations#creating_a_container_cluster).  
   *Note: do not enable Google's [cluster autoscaler](https://cloud.google.com/container-engine/docs/cluster-autoscaler) 
   for a cluster that is controlled by the `gkepool`, since that will cause the 
   cloud pool and the cluster autocscaler to interfere with each other (when their
   views of the correct group target size differ). If you want to autoscale the GKE
   cloud pool you can, for example, use 
   [Elastisys' predictive autoscaler](https://elastisys.com/cloud-platform-features/predictive-auto-scaling/) 
   to control the GKE cloud pool via its REST API.*


Note that node pools can be added/removed to the cluster after creation --
the `gkepool` will continously track node pools as they appear/disappear.




## Configuration
The `gkepool` is configured with a JSON document.

For the cloud-specific parts of the configuration (`cloudApiSettings` 
and `provisioningTemplate`), the `gkepool` requires input similar
to the following:

```javascript
{
    "name": "my-cluster-pool",

    "cloudApiSettings": {
        "serviceAccountKeyPath": "/home/foo/foobar-account-key.json"
    },

    "cluster": {
        "name": "my-cluster",
        "project": "my-project",
        "zone": "europe-west1-c"
    },

    "scalingPolicy": "Balanced",
    
    "alerts": {
	    ...
    },


    "poolUpdateInterval":  {"time": 15, "unit": "seconds"}
}
...
```


The configuration keys have the following meaning:

- `name`:  (**required**): The logical name of the managed pool. 
  When alerts are sent out, this is cloudpool name that will be used.
  Use the name to distinguish between different `gkepools`.

- `cloudApiSettings` (**required**): Sets up API credentials for the [service account](https://developers.google.com/api-client-library/java/google-api-java-client/oauth2#service_accounts) to use.  
  For more details, refer to the [GCE pool README.md](../compute/README.md#service-account-key-configuration)
    - `serviceAccountKeyPath` (*semi-optional*): Local file system path to a 
	  JSON-formatted service account key. May be left out if `serviceAccountKey` is given.
    - `serviceAccountKey` (*semi-optional*): A JSON-formatted service account key. 
	  May be left out, if `serviceAccountKeyPath` is given. This field allows the entire 
	  service account key to be supplied inline.

- `cluster` (**required**): the container cluster being managed.
    - `name`: The name of the container cluster.
	- `project`: The name of the project under which the container cluster has been created.
	- `zone`: The zone where the container cluster is located.

- `scalingPolicy` (*optional*): determines the behavior when the cluster needs to be resized.  
  At the moment, only `Balanced` is supported (described above). Default: `Balanced`.
  
- `alerts` (*optional*): describes how to send alerts via email or HTTP(S) webhooks.  
  For details, refer to the [root-level README.md](../../README.md#configuration).
  
- `poolUpdateInterval` (*optional*): Specifies how often the cloud pool will apply 
  its desired size to the managed cluster  
  Default: `10 seconds`.
  



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
    Default: `/var/lib/elastisys/gkepool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/gkepool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/gkepool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/gkepool`.
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

Here are some examples illustrating basic interactions with the cloud pool (these assume that
the server was started on a HTTP port):


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

