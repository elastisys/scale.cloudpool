# splitter adapter

The splitter adapter makes it easy to work with multiple cloud backends.
It uses a number of other cloud adapters to carry out its operations,
allowing one to work with a single adapter while still making use of
several distinct cloud infrastructures. The backend cloud adapters are
prioritized, making it possible to specify that, e.g., 90% of the 
deployment should be made to one cloud provider, and 10% to some
other provider.

Running several of these splitter adapters, one can create arbitrarily 
complex but useful tree-like configurations, e.g. to deploy a certain 
percentage of machines to Amazon, and some to an internal OpenStack cloud.
Further, the Amazon resources can be split between spot instances and 
on-demand instances.

The splitter adapter adheres to the cloud adapter API, and is used just
like any other.

## Configuration

The splitter cloud adapter is configured with a JSON document such as the
following:

```javascript
{
  "poolSizeCalculator": "STRICT",
    "adapters": [
    {
      "priority": 40,
      "cloudAdapterHost": "cloudadapter-host-1",
      "cloudAdapterPort": 8443,
      "basicCredentials": {
        "username": "admin",
        "password": "adminpassword"
      }
    },
    {
      "priority": 40,
      "cloudAdapterHost": "cloudadapter-host-2",
      "cloudAdapterPort": 8443,
      "basicCredentials": {
        "username": "admin",
        "password": "adminpassword"
      }
    },
    {
      "priority": 20,
      "cloudAdapterHost": "cloudadapter-host-3",
      "cloudAdapterPort": 8443,
      "certificateCredentials": {
        "keystorePath": "/path/to/keystore/goes/here",
        "keystorePassword": "keystorepassword",
        "keystoreType": "PKCS12"
      }
    }
  ]
}
```

The properties are:

 - ``poolSizeCalculator``: Sets which pool size calculation strategy to use,
   i.e., how the adapter should calculate appropriate pool sizes. The 
   currently only supported calculator is ``STRICT``, and it only looks at
   the specified priorities of each backend adapter. It does not take current
   deployment into account.
 - ``adapters``: an array of adapter configurations, consisting of:
  - ``priority``: the priority of the cloud adapter. Must be a positive 
    integer, and all priorities in the configuration file must sum to 100.
  - ``cloudAdapterHost``: the hostname of the cloud adapter REST endpoint;
  - ``cloudAdapterPort``: the port number of the cloud adapter REST endpoint;
  - Credentials for either HTTP Basic or HTTPS authentication, or both, 
    given as:
    - ``basicCredentials``, consisting of ``username`` and ``password``.
    - ``certificateCredentials``, consisting of ``keystorePath`` 
      (path to the keystore file), ``keystorePassword`` (the password to
      unlock the keystore), ``keyPassword`` (the password to unlock the
      key in the keystore -- optional), and ``keystoreType`` (either PKCS12
      or JKS).


## Usage
This module produces an executable jar file for the cloud adapter server.
The simplest way of starting the server is to run

    java -jar <jar-file>

which will start the server on HTTPS port ``8443``. 

*Note: this will run the server on a built-in, self-signed SSL certificate, which is not recommended for production settings.*

For a complete list of options, including the available security options,
run the server with the ``--help`` flag:

    java -jar <jar-file> --help



## Running the cloud adapter in a Docker container
The cloud adapter can be executed inside a 
[Docker](https://www.docker.com/) container. First, however, a docker image 
needs to be built that includes the cloud adapter. The steps for building 
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

If you want to build the image yourself, issue the following commands:

    cd target/
    docker build --tag "elastisys/splitteradapter:<version>" .



### Running a container from the image
Once the docker image is built for the cloud adapter, it can be run with:

    docker run -d -p 2222:22 -p 8443:443 elastisys/splitteradapter:<version>

This will publish the container's SSH port on host port 2222 and the 
cloud adapter's HTTPS port on host port 8443. The container includes a 
privileged user named `elastisys`.

However, password logins are diabled for that user, so if you want to be able to 
log in over SSH some extra effort is needed. More specifically, an ${SSH_KEY} 
environment variable needs to be passed to the container at run-time. The variable 
should be set to the contain a public key (such as ~/.ssh/id_rsa.pub). The key 
will be set in the container's /home/elastisys/.ssh/authorized_keys and therefore 
allow ssh logins by the owner of the corresponding private key. As an example, 
the container could be run as follows:

    docker run -d -p 2222:22 -p 8443:443 \
           -e "SSH_KEY=$(cat ~/.ssh/id_rsa.pub)" \
           elastisys/splitteradapter:<version>

You will then be able to log in to the started container with:

    ssh -i ~/.ssh/id_rsa -p 2222 elastisys@localhost



### Debugging a running container
The simplest way to debug a running container is to log into it over SSH
and check out the log files under `/var/log/elastisys`. Configurations are
located under `/etc/elastisys` and binaries under `/opt/elastisys`.



## Interacting with the cloud adapter over its REST API
The following examples, all using the [curl](http://en.wikipedia.org/wiki/CURL) 
command-line tool, shows how to interact with the cloud adapter over its
[REST API](http://cloudadapterapi.readthedocs.org/en/latest/).

The exact command-line arguments to pass to curl depends on the security
settings that the server was launched with. For example, if client-certificate
authentication is enforced (`--cert-required`), the `<authparams>` parameter 
below can be replaced with:

    --key-type pem --key credentials/client_private.pem \
    --cert-type pem --cert credentials/client_certificate.pem

Here are some examples illustrating basic interactions with the spot adapter:

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

 4. Retrieve the current machine pool (all open spot instance requests):

    ```
    curl -v --insecure <authparams> -X GET https://localhost:8443/pool
    ```

 5. Request the machine pool to be resized to size ``4``:

    ```
    curl -v --insecure <authparams> \
         -X POST -d '{"desiredCapacity": 4}' --header "Content-Type:application/json" \
         https://localhost:8443/pool
    ```


