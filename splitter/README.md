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

This module produces an all-in-one "server and application" 
executable jar that runs an embedded Jetty HTTP server that publishes 
the cloud adapter REST API endpoint.

To run the server, simply issue the following command:

  java -jar <artifact>.jar [arguments ...]
  
This will start a server running listening on HTTPS port 8443.
The port number can be changed (run with the "--help" flag to see the list of 
available options).

If you want to control java.util.logging output produced by embedded libraries,
you can override the default (JRE-provided) log configuration with the 
logging.properties file in this directory. This is done by running: 
 
   java -Djava.util.logging.config.file=./logging.properties -jar <artifact>.jar [arguments ...]

