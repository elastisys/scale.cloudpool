# Google Compute Engine (GCE) cloud pool

The [elastisys](http://elastisys.com/) Google Compute Engine (GCE)
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages the size of a Google Compute Engine 
[managed instance group](https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups).
The instance group, and its [instance template](https://cloud.google.com/compute/docs/instance-templates), 
are assumed to already exist and any load-balancing between group instances,
if needed, is assumed to have been set up (for example, via [GCE's own load-balancer service](https://cloud.google.com/compute/docs/instance-groups/#instance_groups_and_load_balancing)).

Support is provided for both *regional (multi-zone) instance groups*
and *zonal (single-zone) instance groups* (described in the [the official documentation](https://cloud.google.com/compute/docs/instance-groups/#zonal_versus_managed_regional_instance_groups)).
 

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API 
reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).


*Note: for an instance group that is controlled by the GCE cloud pool,
you must not enable GCE [autoscaling](https://cloud.google.com/compute/docs/autoscaler/) 
for the instance group, since that will cause the cloud pool and the 
GCE autocscaler to interfere with each other (when their views of the 
correct group target size differ). If you want to autoscale the GCE 
cloud pool you can, for example, use 
[Elastisys' predictive autoscaler](https://elastisys.com/cloud-platform-features/predictive-auto-scaling/) 
to control the GCE cloud pool via its REST API.*




## Limitations


The GCE cloudpool does not support [attaching instances](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#attach-machine) to an instance group (since that is not supported by regional instance group API).




## Pre-requisites

The GCE cloudpool needs a Google Cloud Platform [service account](https://cloud.google.com/compute/docs/access/service-accounts) to authenticate its API calls to Google Compute Engine.

To set up a service account, follow these steps:


1. [Register for a Google Cloud Platform account](https://cloud.google.com/)

2. [Create a Google Cloud Platform project](https://cloud.google.com/resource-manager/docs/creating-project#via_console)

3. Enable the Google Compute Engine API for your project's [API Manager](https://console.cloud.google.com/apis/dashboard).

4. Set up a service account in your project's [IAM service](https://console.cloud.google.com/iam-admin/iam/), 
   which enables app-level authentication. Since the cloudpool needs to both read and 
   modify cloud resources, choose `Project Editor` as `Role`. You'll also need to create 
   and download a `.json` service account key, which is used to authorise your API calls 
   to Google.

5. [Create an instance group](https://cloud.google.com/compute/docs/instance-groups/creating-groups-of-managed-instances) (and any assets it needs, such as instance template, networks, etc).  
 
    *Note: do not enable GCE [autoscaling](https://cloud.google.com/compute/docs/autoscaler/).*




## Configuration
The `gcepool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings` 
and `provisioningTemplate`), the `gcepool` requires input similar
to the following:

```javascript
    ...	
    "cloudApiSettings": {
        "serviceAccountKeyPath": "/home/foo/foobar-account-key.json"
    },
	
    "provisioningTemplate": {
        "instanceGroup": "webservers",
        "project": "my-project",
        "region": "europe-west1"
    },
...
```


The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `serviceAccountKeyPath` (*semi-optional*): Local file system path to a 
	  JSON-formatted service account key. May be left out if `serviceAccountKey` is given.
    - `serviceAccountKey` (*semi-optional*): A JSON-formatted service account key. 
	  May be left out, if `serviceAccountKeyPath` is given. This field allows the entire 
	  service account key to be supplied inline.

https://developers.google.com/api-client-library/java/google-api-java-client/oauth2#service_accounts

- `provisioningTemplate`: Describes how to provision additional servers (on scale-out).
    - `instanceGroup`: The name of the instance group whose size is to be managed by the cloud pool.
	- `project`: The name of the project under which the instance group was created.
	- `region` (*semi-optional*): The region where the instance group is located, in case the instance
      group is regional/multi-zone. If the instance group is a zonal one, specify `zone` instead.
	- `zone` (*semi-optional*): The zone where the instance group is located, in case the instance group
     * is zonal/single-zone. If the instance group is a multi-zone one, specify `region` instead.
  
  

### Service account key configuration
The json service account key can either be specified inline or as a file reference.

Example of passing `serviceAccountKey` inline:

```javascript

    ...	
    "cloudApiSettings": {
        "serviceAccountKey": {
            "type": "service_account",
            "project_id": "sample-project",
            "private_key_id": "abcdefabcdefabcdefabcdefabcdefabcdefabcd",
            "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDS9h5R2t5XNkon\nXwulveitQE/CHR3IY1wrrIuAtjhr6F+OtBngP8AJjAWz3z2hCVRw1puJr25y+7MD\nncp90tiPlx+e2kPj74+QYWYw/C613PWhmKqZbQDB8uEFuvj4rllujstM18Fr6Xtu\noEpp5UNqsBK/9zIwPKjRf7TG26oexi8ptB1AdvgsyMRiMFqp7DZ+USY/N1OfazIY\nOcd0TYE73k+gczK/LZTCZZLFBsk6OL11fKPcO5iDDaZHPj0HaI3hw1Z7gi6oRAsU\nmIMn7Owbs4AIXD3wIR67zjH81R5bbiQFdRLLqgEJCkx+8Xg2ZxmOSIV6Tki/4a93\n3FK7RmK/AgMBAAECggEBAMqVGiyUtyqcsbz5IqA4rDWjKnRHrY//CkXOXfBuC+Rj\ns0/AV5Dm5yzDz74ZRH9eDD6Hd7lSRAga+J8iaY9GaDwLbYwIIWXDaOPHBHFJ2jk+\nRIq3pivLZwzYhYoRMIDuPGdUrJnQHqfYAHYNayghxwmup3K8mO+FcMAtmJXV35nI\nC8XpixoI+2Do472viiK1iNdWzqVe0iVHxEmkacf6YnbpghXUyXPc6u6pXNZKMuvU\n/qeAWlj83nFieTclMh6AhMgBfxu8fAueq7wgJ/E1XXbMPW7husn+PbrOpzUPP5im\nWlruOXwwJSKwkbdr23Ri03cCRIalRsHp+hg3UdtAnRECgYEA984+1LIQ9kyLeABA\ngSOujx1NxR841/HP1wM4O5GbXslQ2DByob1CA5hyOXJ3OBL3P0yjUx6zUGqjcSSX\nzlnOyBCcmpX/cU1X8MtainVPnWLVt/q4roF51mjorAHQltYci5AviclRev0rnE+5\n+/iM7FwnyR1nFgRk0+nlL5Ut290CgYEA2e/5f2AAVxyxfK5EZQXdJvsXzsZNYhub\nfvAJEvlaQMV4RP3uQ7OGYew1gkcu40oPU8mvCz3MyO+1jmInexHaGE8UZm+nPIdr\nq4jBpPYmb9dTridE563g3/k5t7jh3pRJzDHXmTBi2a8iaiduy4mTj5qEAdQKUD4/\nKoJE1tfzzUsCgYAQWI92ckSCKqxsfv/CRPtHv3QY06VpQS8njLPR4hnwl7D4gcGd\n/6DyIcUvGujYTw/2IfUID5deb5pWZUaKOwxT0By0ab6O1748SP2w8moNBK76UtSJ\n1kqWJRdr+TasLHV3k0w/d4MjbL3Kssue3ldVZALP1lutbR7Fh8ExcVK0FQKBgDUJ\ndpFt/oc0n2j1TAW9yJUs9B1JGxnuGAhrR/oLHtC2fc9kcLCfJuv7vQXoZtu/cT11\nzpiQwJ6B35A4CN2leYsC67cGIw/S8Pv8uwt3L+F16JJwUg+DvD8lestgPe+Z7N+5\nnXtVUCkjut9Um10DLRr8gnWjcaMXku24aCJUyatDAoGBALt3MiHqo54A5qZfRInz\np34C5/jQursCJ3Bf90eG9GVSYgxtayLrhfsxymSC8tL7ZqYi5N74AiyJ8Aw8TauY\n8kIUL7PRcnMrT6sEgVBhL3GRq5mk2GUioNBwhIV1oTo/L5OWJvEodh14sNaAhvzs\neuKxO+IIk9ZDYkPlikFQYPma\n-----END PRIVATE KEY-----\n",
            "client_email": "bot@esample-project.iam.gserviceaccount.com",
            "client_id": "123456789012345678901",
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://accounts.google.com/o/oauth2/token",
            "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
            "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/bot%40sample-project.iam.gserviceaccount.com"
    },

```

Example of passing service account key as a file via `serviceAccountKeyPath`:

```javascript

    "cloudApiSettings": {
        "serviceAccountKeyPath": "/home/foo/foobar-account-key.json"
    },
	...
```


### Configuring multi-zone vs single-zone instance groups

As already mentioned, the GCE cloud pool supports both *regional (multi-zone) instance groups*
and *zonal (single-zone) instance groups* (described in the [the official documentation](https://cloud.google.com/compute/docs/instance-groups/#zonal_versus_managed_regional_instance_groups)).

A multi-zone instance group must specify the `region` field in the `provisioningTemplate`:

    "provisioningTemplate": {
        "instanceGroup": "webservers",
        "project": "my-project",
        "region": "europe-west1"
    },


A single-zone instance group, on the other hand, must specify the `zone` field in the `provisioningTemplate`:

    "provisioningTemplate": {
        "instanceGroup": "webservers",
        "project": "my-project",
        "zone": "europe-west1-d"
    },


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
    Default: `/var/lib/elastisys/gcepool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/gcepool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/gcepool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/gcepool`.

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
authentication is enforced (`--require-cert`), the `<authparams>` parameter 
below can be replaced with:

    --key-type pem --key credentials/client_private.pem \
    --cert-type pem --cert credentials/client_certificate.pem

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
