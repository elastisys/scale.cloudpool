scale.cloudpool
===============

The ``scale.cloudpool`` project contains Java-based 
[elastisys](http://elastisys.com/) 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) implementations 
for different cloud providers:

  - [OpenStack](openstack/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [OpenStack](https://www.openstack.org/) servers.
  - [CityCloud](citycloud/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [OpenStack](https://www.openstack.org/) servers in City Cloud 
    (an OpenStack-based cloud provider that uses Keystone v3 authentication).
  - [AWS EC2](aws/ec2/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [AWS EC2](http://aws.amazon.com/ec2/) instances.
  - [AWS Spot](aws/spot/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [AWS Spot](http://aws.amazon.com/ec2/spot/) instances.
  - [AWS Auto Scaling Group](aws/autoscaling/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages the size of an [AWS Auto Scaling Group](http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/WorkingWithASG.html).
  - [Microsoft Azure](azure/README.md): a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [Microsoft Azure](https://azure.microsoft.com/en-us/) VMs.

The [cloudpool.commons](commons) module contains a generic `CloudPool` implementation
(`BaseCloudPool`) intended to be used as a basis for building cloud-specific cloud pools.


Getting started
===============

This project depends on [scale.commons](https://github.com/elastisys/scale.commons). If you are building from `master` yourself (where the POM file refers to a SNAPSHOT version), you need to clone and build that code repository first.

Once that has been installed using Maven, this project can be built with:

  `mvn clean install`


For each of the cloud pool implementation modules, the build produces an 
executable jar file that starts an HTTP(S) server that publishes the cloud
pool [REST API](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html).

To start a server, simply execute the jar file:

  `java -jar <artifact>.jar --https-port 8443 --config <JSON file>`
  
This will start a server running listening on HTTPS port 8443.
The port number can be changed (run with the ``--help`` flag to see the list of 
available options).

The behavior of the cloud pool is controlled through a JSON-formatted 
configuration document and can either be set at start-up time with the
``--config`` command-line flag, or over a REST API in case the server
has been started with the ``--config-handler`` command-line option.
A configuration can be set over the REST API by a ``POST`` of the document 
to  the ``/config`` endpoint.

The JSON document that is passed as configuration is outlined below.


Configuration
=============
The configuration document follows the same schema for all cloud pool 
implementations in this project. There are two parts of the configuration
document that carries cloud provider-specific settings:

- The `cloudPool/driverConfig` element typically declares credentials needed
  to connect to the cloud provider API. The exact properties that each specific
  cloud pool implementation. Refer to the `README.md` for a particular cloudpool
  implementation for details.
- The `scaleOutConfig/extensions` (if set) contains cloud provider-specific
  server provisioning parameters.

Below is a sample configuration document for the ``cloudpool.aws.ec2``:

```javascript
  {
    "cloudPool": {
      "name": "MyEc2InstanceScalingPool",
      "driverConfig": {
        "awsAccessKeyId": "AXZ31...Q",
        "awsSecretAccessKey": "afAC/3Dd...s",
        "region": "eu-west-1"
      }
    },
    "scaleOutConfig": {
      "size": "m1.small",
      "image": "ami-982bc6f0",
      "keyPair": "instancekey",
      "securityGroups": ["webserver"],
      "encodedUserData": "<base-64 encoded data>",
      "extensions": {
         ... cloud-provider specific provisioning parameters ...
      }
    },
    "scaleInConfig": {
      "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
      "instanceHourMargin": 300
    },
    "alerts": {
      "duplicateSuppression": { "time": 5, "unit": "minutes" },
      "smtp": [
        {
          "subject": "[elastisys:scale] cloud pool alert for MyScalingPool",
          "recipients": ["receiver@destination.com"],
          "sender": "noreply@elastisys.com",
          "smtpClientConfig": {
            "smtpHost": "mail.server.com",
            "smtpPort": 465,
            "authentication": {"userName": "john", "password": "secret"},
            "useSsl": True
          }
        }
      ],
      "http": [
        {
          "destinationUrls": ["https://some.host1:443/"],
          "severityFilter": "ERROR|FATAL",
          "auth": {
            "basicCredentials": { "username": "user1", "password": "secret1" }
          }
        },
        {
          "destinationUrls": ["https://some.host2:443/"],
          "severityFilter": "INFO|WARN", 
          "auth": {
            "certificateCredentials": { "keystorePath": "src/test/resources/security/client_keystore.p12", "keystorePassword": "secret" }
          }
        }
      ]
    },
    "poolFetch": {
      "retries": { 
         "maxRetries": 3, 
         "initialBackoffDelay": {"time": 3, "unit": "seconds"}
      },
      "refreshInterval": {"time": 30, "unit": "seconds"},
      "reachabilityTimeout": {"time": 5, "unit": "minutes"}
    },
    "poolUpdate": {
      "updateInterval": {"time": 1, "unit": "minutes"}
    }
  }
```

The configuration document declares how the cloud pool:

  - should configure its cloud-specific `CloudPoolDriver` class to allow it to communicate with its cloud API (the ``driverConfig`` key).

  - provisions new machines when the pool needs to grow (the ``scaleOutConfig`` key).

  - decommissions machines when the pool needs to shrink (the ``scaleInConfig`` key).

  - alerts system administrators (via email) of resize operations, error conditions, etc (the ``alerts`` key).




In a little more detail, the configuration keys have the following meaning:

  - ``cloudPool`` (**required**): Describes how to identify/manage pool members 
    and connect to the cloud provider.

    - ``name`` (**required**): The logical name of managed machine pool. The exact way of 
      identifying pool members may differ between implementations, but 
      machine tags could, for example, be used.

    - ``driverConfig`` (**required**): `CloudPoolDriver`-specific JSON configuration 
	  document, the contents of which depends on the particular 
	  ``CloudPoolDriver``-implementation 
      being used. Typically, a minimum amount of configuration includes login 
      credentials for connecting to the particular cloud API endpoint. The 
      properties expected by a certain cloud pool implementation can be
      found in their JSON schema under ``src/main/resources``.

  - ``scaleOutConfig`` (**required**): Describes how to provision additional servers (on scale-out).

    - ``size`` (**required**): The name of the server type to launch. For example, ``m1.medium``. 
	
    - ``image`` (**required**): The name of the machine image used to boot new servers.

    - ``keyPair`` (*optional*): The name of the key pair to use for new machines.

    - ``securityGroups`` (*optional*): The security group(s) to use for new machines.

    - ``encodedUserData`` (*optional*): A 
	  [base64-encoded](http://tools.ietf.org/html/rfc4648)
      blob of data used to pass custom data to started machines typically in
      the form of a boot-up shell script or cloud-init parameters. Can, for
      instance, be produced via `cat bootscript.sh | base64 -w 0`.
      Refer to the [Amazon documentation](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html) for details.
	  
    - ``extensions`` (*optional*): An extension element for cloud-specific functionality. 
	  A `CloudPoolDriver` may choose to parse this section of the provisioning template 
	  for cloud-specific behavior.

  - ``scaleInConfig`` (**required**): Describes how to decommission servers (on scale-in).

    - ``victimSelectionPolicy`` (**required**): Policy for selecting which machine to 
      terminate. Allowed values: ``NEWEST_INSTANCE``, ``OLDEST_INSTANCE``, 
      ``CLOSEST_TO_INSTANCE_HOUR``.

    - ``instanceHourMargin`` (**required**): How many seconds prior to the next instance 
	  hour an acquired machine should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-in is ordered.

  - ``alerts`` (*optional*): Configuration that describes how to send alerts via email or HTTP(S) webhooks.
    - ``duplicateSuppression`` (optional): Duration of time to suppress
      duplicate alerts from being re-sent. Two alerts are considered equal if
      they share topic, message and metadata tags.  
    - ``smtp``: a list of email alert senders
      - ``subject``: The subject line to use in sent mails (Subject).
      - ``recipients``: The receiver list (a list of recipient email addresses).
      - ``sender``: The sender email address to use in sent mails (From).
      - ``severityFilter``: A regular expression used to filter alerts. Alerts 
        with a severity (one of ``DEBUG``, ``INFO``, ``NOTICE``, ``WARN``, 
        ``ERROR``, ``FATAL``) that doesn't match the filter expression are 
        suppressed and not sent. Default: ``.*``.
        - ``smtpClientConfig``: Connection settings for the SMTP client.
          - ``smtpHost``: SMTP server host name/IP address.
          - ``smtpPort``: SMTP server port. Default is 25.
          - ``authentication``: Optional username/password to authenticate with SMTP
            server. If left out, authentication is disabled.
          - ``useSsl``: Enables/disables the use of SSL for SMTP connections. Default 
            is false (disabled).
    - ``http``: a list of HTTP(S) webhook alert senders, which will ``POST`` alerts
       to the specified endpoint using the (optional) configured authentication 
       credentials.
      - ``destinationUrls``: The list of destination endpoint URLs.
      - ``severityFilter``: A regular expression used to filter alerts. Alerts 
        with a severity (one of ``DEBUG``, ``INFO``, ``NOTICE``, ``WARN``, 
        ``ERROR``, ``FATAL``) that doesn't match the filter expression are 
        suppressed and not sent. Default: ``.*``.
      - ``auth``: Authentication credentials. Can specify either ``basicCredentials``
        or ``certificateCredentials`` or both.
        - ``basicCredentials``: ``username`` and ``password`` to use for BASIC-style
          authentication.
        - ``certificateCredentials``: ``keystorePath`` and ``keystorePassword``
          for client certificate-based authentication.
  - ``poolFetch`` (*optional*): Controls how often to refresh the cloud 
    pool member list and for how long to mask cloud API errors. 
    Default: ``retries``: 3 retries with 3 second initial exponential back-off delay,
    ``refreshInterval``: 30 seconds, ``reachabilityTimeout``: 5 minutes.
    - ``retries``: Retry handling when fetching pool members from the cloud API fails.
      - ``maxRetries``: Maximum number of retries to make on each attempt to fetch pool 
        members.
      - ``initialBackoffDelay``: Initial delay to use in exponential back-off on retries. 
        May be zero, which results in no delay between retries.        
    - ``refreshInterval``: How often to refresh the cloud pool's view of the machine 
      pool members.
    - ``reachabilityTimeout``: How long to respond with cached machine pool observations
      before responding with a cloud reachability error. In other words, for how long should
      failures to fetch the machine pool be masked.              
  - ``poolUpdate`` (*optional*): Controls the behavior with respect to how often to 
    attempt to update the size of the machine pool to match the desired size.
    - ``updateInterval``: The time interval between  periodical pool size updates. 
      Default: 60 seconds.


Multi-cloud support
===================

Elastisys has also developed a Splitter cloud pool implementation that can be 
used to manage several clouds as one, complete with fail-over functionality 
built in. It adheres to the exact same API as the provider-specific ones. 
Users of the Splitter cloud pool defines a splitting policy, such as 
"90 percent AWS Spot instances, 10 percent AWS EC2 instances", and the 
Splitter takes care of maintaining this ratio.

Should a cloud fail to provide an instance fast enough (for whatever reason), 
the Splitter cloud pool will obtain an equivalent instance from another of 
its configured cloud backends. Once the original cloud provider is operating
as intended again, the Splitter will automatically decommission the replacement
machine from the other cloud backend.

Some of our customers use it to ensure that their services are highly available, 
even in the face of cloud provider failure. Others use it to run mostly Spot
instances, and fall back to on-demand instances when Spot instances are scarce.

The Splitter cloud pool is not open source at this time, so 
[contact Elastisys](http://elastisys.com/contact/) if you would like to
discuss how the Splitter cloud pool can help optimize your cloud deployment.

License
=======
Copyright (C) 2013 Elastisys AB

Licensed under the Apache License, Version 2.0

