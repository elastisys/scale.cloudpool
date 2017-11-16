# AWS Spot Instance cloud pool

The [elastisys](http://elastisys.com/) AWS Spot Instance
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of AWS spot  instances. Pool members are identified by a
configurable tag and instances are continuously provisioned/decommissioned to
keep the pool's actual size in sync with the desired size that the cloud
pool has been instructed to maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API
reference, the reader is referred to the
[cloudpool documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).

*Note that if you plan on using a large number of spot instances for your
service, you may need to contact Amazon to have them raise the
[spot request usage limit](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-spot-limits.html) for
your account. Although the documentation states a maximum of 20 ongoing spot
requests per region, tests have shown that 50 requests were possible (in the
us-east-1 region). To submit a limit increase request, go to
[AWS Support Center](https://console.aws.amazon.com/support/home#/case/create?issueType=service-limit-increase&limitType=service-code-ec2-instances) and
complete the request form. In the Use Case Description field, indicate that you
are requesting an increase to the request limit for Spot instances.*


## Configuration
The `spotpool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings`
and `provisioningTemplate`), the `spotpool` requires input similar
to the following:

```javascript
    ...
    "cloudApiSettings": {
        "awsAccessKeyId": "ABC...123",
        "awsSecretAccessKey": "DEF...456",
        "region": "eu-west-1",
        "bidPrice": 0.005,
        "bidReplacementPeriod": { "time": 2, "unit": "minutes" },
        "danglingInstanceCleanupPeriod": { "time": 2, "unit": "minutes" },
        "connectionTimeout": 10000,
        "socketTimeout": 10000
    },

    "provisioningTemplate": {
        "instanceType": "m1.small",
        "amiId": "ami-da05a4a0",
        "subnetIds": [ "subnet-44b5786b", "subnet-dcd15f97" ],
        "assignPublicIp": true,
        "keyPair": "instancekey",
        "iamInstanceProfileARN": "arn:aws:iam::123456789012:instance-profile/my-iam-profile",
        "securityGroupIds": ["sg-12345678"],
        "encodedUserData": "<base-64 encoded data>",
        "ebsOptimized": false,
        "tags": {
            "Cluster": "mycluster"
        }
    },
	...
```

The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `awsAccessKeyId`:	Your
      [AWS Access Key ID](https://aws-portal.amazon.com/gp/aws/securityCredentials).
    - `awsSecretAccessKey`: Your
      [AWS Secret Access Key](https://aws-portal.amazon.com/gp/aws/securityCredentials).
    - `region`:
      The [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html)
      to connect to.
    - `bidPrice`: The bid price (maximum price to pay for an instance hour in
      dollars) to use when requesting spot instances.
    - `bidReplacementPeriod`: The delay between two successive bid replacement
      runs (replacing spot requests with an out-dated bid price).
    - `danglingInstanceCleanupPeriod`: The delay between two successive
      dangling instance cleanup runs (where instances whose spot requests have
      been canceled are terminated).
    - `connectionTimeout`: The timeout in milliseconds until a connection is
      established.
    - `socketTimeout`: The socket timeout (`SO_TIMEOUT`) in milliseconds, which
	  is the timeout for waiting for data or, put differently, a maximum period
	  inactivity between two consecutive data packets.

- `provisioningTemplate`: Describes how to provision additional instances (on
  scale-up).
    - `instanceType`: The name of the
      [instance type](https://aws.amazon.com/ec2/instance-types/)
	  to launch. For example, `t2.small`.
    - `image`:
      The [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) to
      use to boot new machine instances. For example, `ami-da05a4a0`.
	- `subnetIds`: The IDs of one or more VPC subnets into which created
      instances are to be launched. The subnets also implicitly determine the
      availability zones that instances can be launched into. At least one
      subnet must be specified. A best effort attempt will be made to place
      instances evenly across the subnets. *All subnets are assumed to belong to
      the same VPC*.
	- `assignPublicIp`: Indicates if created instances are to be assigned a
      public IP address.
    - `keyPair`: The name of the key pair to use for new instances. If you do
      not specify a key pair, you can't connect to the instance unless you
      choose an AMI that is configured to allow users another way to log in.
	- `iamInstanceProfileARN`: The ARN of an IAM instance profile to assign to
      created instances. May be left out if no IAM role is to be delegated to
      instances. For example,
      `arn:aws:iam::123456789012:instance-profile/my-iam-profile`.
    - `securityGroupIds`: Zero or more security group IDs to apply to created
      instances. If none is specified, EC2 will assign a default security
      group.*All security groups are assumed to belong to the VPC of the
      `subnetIds`.*.
    - `encodedUserData`: A [base64](http://tools.ietf.org/html/rfc4648)-encoded
      blob of data used to pass custom data (such as a boot script) to started
      instances. Refer to the
      [AWS documentation](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html)
	  for details. May be <code>null</code>.
    - `ebsOptimized`: Indicates whether created instances are to be optimized
      for Amazon EBS I/O. This optimization provides dedicated throughput to
      Amazon EBS and an optimized configuration stack to provide optimal Amazon
      EBS I/O performance. Note that this optimization isn't available with all
      instance types. Additional usage charges apply when using an EBS-optimized
      instance.
	- `tags`: Tags that are to be set on created instances in addition to the
	  `elastisys:CloudPool` tag (used to determine pool membership) and the
	  `Name` tag that are always set by the cloudpool.




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
    Default: `/var/lib/elastisys/spotpool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).
    Default: `/etc/elastisys/spotpool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.
    Default: `/etc/elastisys/spotpool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).
    Default: `/var/log/elastisys/spotpool`.
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
