# Azure cloud pool

The [elastisys](http://elastisys.com/) Azure
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/)
manages a pool of Azure Virtual Machines (VMs). 

The cloud pool is capable of running both Linux and Windows VMs.

Pool members are identified by a configurable tag and servers are 
continuously provisioned/decommissioned to keep the pool's actual size
in sync with the desired size that the cloud pool has been instructed to 
maintain.

The cloud pool publishes a REST API that follows the general contract of an
[elastisys](http://elastisys.com/) cloud pool, through which
a client (for example, an autoscaler) can manage the pool. For the complete API 
reference, the reader is referred to the 
[cloud pool API documentation](http://cloudpoolrestapi.readthedocs.org/en/latest/).




## Limitations

The Azure cloud pool only supports the *Resource Manager* deployment model and 
not the *Classic* deployment model. For more details on what this means, refer to 
[this discussion of the deployment models](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-manager-deployment-model). In essence, the Classic model is the old deployment
model and the Resource Manager model is the way to go, unless you have some legacy
application. As the aforementioned article states:

> To simplify the deployment and management of resources, Microsoft 
> recommends that you use Resource Manager for all new resources. If 
> possible, Microsoft recommends that you redeploy existing resources through 
> Resource Manager.



## Pre-requisites
Before you start using the Azure cloud pool, you need to set up an Azure 
account and a subscription and configure the Azure cloud pool to make use
of that subscription.

1. First, you need to sign up for an [Azure account](https://azure.microsoft.com/).

2. Within that account set up a subscription, which will be charged for the 
   VMs/assets created by the cloud pool.

3. You also need to create a [service principal registration](https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md) for the cloudpool. This will give the 
   cloudpool credentials to operate on behalf of your account.
   
    *Note: instructions use somewhat confusing and contradicting terms for
    certain concepts.*

    A suitable [role](https://docs.microsoft.com/en-us/azure/active-directory/role-based-access-built-in-roles) for the service principal is `Contributor`.

    *Note: the registration process may not be instantaneous.
    Therefore, it may take a while before the service principal credentials
    start working.*
	
	
Before you start a cloud pool you should create the assets that will be 
referenced by the cloud pool's VM provisioning template.
	
1. Create a [resource group](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview) that will contain the pool VMs and assets they reference 
  (network interfaces, virtual networks, security groups, etc).


2. In your resource group, and for the [location]() (region) you're planning for
   your pool, you may want to create some additional assets to be used by your
   pool VMs:
   
       1. Create a [virtual network](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-overview) to which your VMs will be attached.
	   2. Within the virtual network, create a [subnet](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-overview#subnets), from which the VM will have its private IP address assigned.
       3. You may want to create one or more [network security groups](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-overview#network-security-groups-nsg) 
	      (firewall rules).
       4. Create a [storage account](https://docs.microsoft.com/en-us/azure/storage/storage-create-storage-account), which will be used to store the OS disk VHD
	      of started VMs.  
		  *Note: when the pool terminates a VM, it will take care of removing the OS 
		  disk.*


## Configuration
The `azurepool` is configured with a JSON document which follows the general
structure described in the [root-level README.md](../README.md).

For the cloud-specific parts of the configuration (`cloudApiSettings` 
and `provisioningTemplate`), the `azurepool` requires input similar
to the following:

```javascript
    ...	
    "cloudApiSettings": {
        "apiAccess": {
            "subscriptionId": "12345678-9abc-def0-1234-56789abcdef0",
            "auth": {
                "clientId": "12345678-9abc-def0-1234-56789abcdef0",
                "domain": "12345678-9abc-def0-1234-56789abcdef0",
                "secret": "12345678-9abc-def0-1234-56789abcdef0",
                "environment": "AZURE"
            },
            "connectionTimeout": { "time": 10, "unit": "seconds" },
            "readTimeout": { "time": 10, "unit": "seconds" },
            "azureSdkLogLevel": "NONE"
        },
        "resourceGroup": "testpool",
        "region": "northeurope"
    },
	
    "provisioningTemplate": {
        "size": "Standard_DS1_v2",
        "image": "Canonical:UbuntuServer:16.04.0-LTS:latest",
        "extensions": {
            "linuxSettings": {
                "rootUserName": "ubuntu",
                "publicSshKey": "ssh-rsa XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX foo@bar",
                "customScript": {
                    "encodedCommand": "c2ggLWMgJ2FwdCB1cGRhdGUgLXF5ICYmIGFwdCBpbnN0YWxsIC1xeSBhcGFjaGUyJwo="
                }
            },
            "storageAccountName": "testpooldisks",
            "network": {
                "virtualNetwork": "testnet",
                "subnetName": "default",
                "assignPublicIp": true,
                "networkSecurityGroups": ["webserver"]
            },
            "tags": {
                "tier": "web"
            }
        }
    },
...
```


The configuration keys have the following meaning:

- `cloudApiSettings`: API access credentials and settings.
    - `apiAccess`: Azure [service principal](https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md) access credentials and settings:
        - `subscriptionId`: The Azure account subscription that will be billed allocated resources. Format: `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`.
        - `auth`: Azure API authentication credentials.
		  - `clientId`: The active directory client id for this application. May also be referred to as `appId`. Format: `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`.
		  - `domain`: The domain or tenant id containing this application. Format: `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`.
		  - `secret`: The authentication secret (password) for this application. Format: `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`.
		  - `environment` (*optional*): The particular Azure environment to authenticate against. One of: `AZURE`, `AZURE_CHINA`, `AZURE_US_GOVERNMENT`, `AZURE_GERMANY`. Default: `AZURE`, the world-wide public Azure cloud.
        - `connectionTimeout` (*optional*): The timeout until a connection is established. Default: `10 seconds`.
        - `socketTimeout` (*optional*): The socket timeout (`SO_TIMEOUT`), which is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets. Default: `10 seconds`.
		- `azureSdkLogLevel` (*optional*): The log level to see REST API requests made to the Azure API. One of `NONE`, `BASIC`, `HEADERS`, and `BODY`. Default: `NONE`.
    - `resourceGroup`: The name of the [resource group](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-overview) that contains referenced resources (VMs, networks, security groups, etc). *Note: this asset must have been created in advance.*
    - `region`: The region where pool VMs and referenced assets (networks, security groups, images, etc) are located in. For example, `northeurope`.

- `provisioningTemplate`: Describes how to provision additional servers (on scale-out).
    - `size`: The name of the server type to launch. For example, `Standard_DS1_v2`.
    - `image`: The name of the machine image used to boot new servers. For example, `Canonical:UbuntuServer:16.04.0-LTS:latest`.
    - `extensions`: Azure-specific VM provisioning settings
	  - `vmNamePrefix` (*optional*): Name prefix to assign to created VMs. The cloudpool will add a VM-unique suffix to the prefix to produce the final VM name: `<vmNamePrefix>-<suffix>`. If left out, the default is to use the cloud pool's name as VM name prefix.
	  - `linuxSettings` (*semi-optional*): Settings particular to launching Linux VMs. Either this or `windowsSettings` must be specified.
	    - `rootUserName` (*optional*): Name of the root Linux account to create on created VMs. Default: `root`.
		- `publicSshKey` (*semi-optional*): An OpenSSH public key used to login to created VMs. Must be given unless `password` is specified.
		- `password` (*semi-optional*): A password used to login to created VMs. Must be given unless `publicSshKey` is specified.
		- `customScript` (*optional*): a (set of) custom script(s) to run when a VM is booted.
		  - `fileUris` (*optional*): A set of file URIs to download before executing the script.
		  - `encodedCommand`: A base64-encoded command to execute. Such a command can, for example, be generated via a call similar to: `echo "sh -c 'apt update -qy && apt install -qy apache2'" | base64 -w0`.
	  - `windowsSettings` (*semi-optional*): Settings particular to launching Windows VMs. Either this or `linuxsSettings` must be specified.
	    - `adminUserName`: The administrator user name for the Windows VM. Default: `windowsadmin`.
		- `password`: A password used to login to created VMs.
		- `customScript` (*optional*): a (set of) custom script(s) to run when a VM is booted.
		  - `fileUris` (*optional*): A set of file URIs to download before executing the script.
		  - `encodedCommand`: A base64-encoded command to execute. Such a command can, for example, be generated via a call similar to: `echo "powershell.exe -ExecutionPolicy Unrestricted -File install-webserver.ps1" | base64 -w 0`
	  - `storageAccountName`: An existing storage account used to store the OS disk VHD for created VMs.
	  - `network`: Network settings for created VMs.
	    - `virtualNetwork`: An existing virtual network that created VMs will be attached to (the VM's primary network interface will receive a private IP address from this network).
	    - `subnetName`: The subnet within the virtual network, from which a (private) IP address will be assigned to created VMs.
	    - `assignPublicIp` (*optional*): Set to `true` to assign a public IP address to created VMs. Default: `false`.
	    - `networkSecurityGroups` (*optional*): A set of existing network security groups to associate with created VMs. May be {@code null}, which means that no security groups get associated with the primary network interface of created VMs. The default behavior is to allow all inbound traffic from inside the VM's virtual network and to allow all outbound traffic from a VM.
	  - `tags` (*optional*): Tags to associate with created VMs. *Note: a `elastisys-CloudPool` tag will automatically be set on each pool VM and should not be overridden*.
	  



## Booting Windows VMs

The example above illustrated how to configure the pool to boot Linux VMs. 
To set up a pool of Windows VMs, replace the `linuxSettings` element in the
`provisioningTemplate` with a `windowsSettings` similar to the following:

```javascript

            "windowsSettings": {
                "adminUserName": "mysupersecretuser",
                "password": "Oz5ichahyaen",
                "customScript": {
                    "fileUris": ["https://gist.githubusercontent.com/elastisys/09be421f09ae3646f1aadf4542f6b8f2/raw/e42334045905f908d781e78e03bb9412bf325da7/win-server-install-webserver.ps1"],
                    "encodedCommand": "cG93ZXJzaGVsbC5leGUgLUV4ZWN1dGlvblBvbGljeSBVbnJlc3RyaWN0ZWQgLUZpbGUgd2luLXNlcnZlci1pbnN0YWxsLXdlYnNlcnZlci5wczEK"
                }
            }
```

In this case, the VM starts a IIS web server when it boots.




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
    Default: `/var/lib/elastisys/azurepool`.


Debug-related:

  - `LOG_CONFIG`: [logback](http://logback.qos.ch/manual/configuration.html)
    logging configuration file (`logback.xml`).  
    Default: `/etc/elastisys/azurepool/logback.xml`.
  - `JUL_CONFIG`: `java.util.logging` `logging.properties` configuration.  
    Default: `/etc/elastisys/azurepool/logging.properties`.
  - `LOG_DIR`: destination folder for log files (when using default
    `${LOG_CONFIG}` setup).  
    Default: `/var/log/elastisys/azurepool`.

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
