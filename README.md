scale.cloudpool
===============

The ``scale.cloudpool`` project contains Java-based 
[elastisys:scale](http://elastisys.com/scale) 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) implementations 
for different cloud providers:

  - ``cloudadapers.openstack``: a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [OpenStack](https://www.openstack.org/) servers.
  - ``cloudadapers.aws.ec2``: a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages a group of [AWS EC2](http://aws.amazon.com/ec2/) instances.
  - ``cloudadapers.aws.autoscaling``: a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages the size of an [AWS Auto Scaling Group](http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/WorkingWithASG.html).
  - ``cloudadapers.splitter``: a [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) 
    that manages the size of several backend cloud pools.


Getting started
===============

Clone the repository and build it with:

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
The configuration document follows the same schema for all cloud pools, 
except for one part that holds cloud provider-specific settings. This part 
of the configuration is stored in the ``cloudPool`` section under the
``driverConfig`` key. This configuration key typically contains credentials needed
to connect to the cloud provider API. The exact properties that each specific
cloud pool implementation expects can be found in their JSON schema under
``src/main/resources``. Sample configuration documents for each cloud pool
can typically be found under ``src/test/resources``.

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
      "bootScript": [
        "#!/bin/bash",
        "sudo apt-get update -qy",
        "sudo apt-get install -qy apache2"
      ]
    },
    "scaleInConfig": {
      "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
      "instanceHourMargin": 300
    },
    "alerts": {
      "subject": "[elastisys:scale] cloud pool alert for MySpotScalingPool",
      "recipients": ["receiver@destination.com"],
      "sender": "noreply@elastisys.com",
      "severityFilter": "INFO|NOTICE|WARN|ERROR|FATAL",
      "mailServer": {
        "smtpHost": "smtp.host.com",
        "smtpPort": 465,
        "authentication": {"userName": "johndoe", "password": "secret" },
        "useSsl": true
      }
    },
    "poolUpdatePeriod": 120
  }
```

The configuration document declares how the cloud pool:

  - should configure its cloud-specific `CloudPoolDriver` class to allow it to communicate with its cloud API (the ``driverConfig`` key).

  - provisions new instances when the pool needs to grow (the ``scaleOutConfig`` key).

  - decommissions instances when the pool needs to shrink (the ``scaleInConfig`` key).

  - alerts system administrators (via email) of resize operations, error conditions, etc (the ``alerts`` key).




In a little more detail, the configuration keys have the following meaning:

  - ``cloudPool``: Describes how to identify/manage pool members 
    and connect to the cloud provider.

    - ``name``: The logical name of managed machine pool. The exact way of 
      identifying pool members may differ between implementations, but 
      instance tags could, for example, be used.

    - ``driverConfig``: `CloudPoolDriver`-specific JSON configuration document, the 
      contents of which depends on the particular ``CloudPoolDriver``-implementation 
      being used. Typically, a minimum amount of configuration includes login 
      credentials for connecting to the particular cloud API endpoint. The 
      properties expected by a certain cloud pool implementation can be
      found in their JSON schema under ``src/main/resources``.

  - ``scaleOutConfig``: Describes how to provision additional servers (on scale-out).

    - ``size``: The name of the server type to launch. For example, ``m1.medium``.

    - ``image``: The name of the machine image used to boot new servers.

    - ``keyPair``: The name of the key pair to use for new machine instances.

    - ``securityGroups``: The security group(s) to use for new machine instances.

    - ``bootScript``: The script to run after first boot of a new instance.

  - ``scaleInConfig``: Describes how to decommission servers (on scale-in).

    - ``victimSelectionPolicy``: Policy for selecting which spot instance to 
      terminate. Allowed values: ``NEWEST_INSTANCE``, ``OLDEST_INSTANCE``, 
      ``CLOSEST_TO_INSTANCE_HOUR``.

    - ``instanceHourMargin``: How many seconds prior to the next instance hour 
      an acquired machine instance should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-down is ordered.

  - ``alerts``: Configuration that describes how to send email alerts.
 
    - ``subject``: The subject line to use in sent mails (Subject).

    - ``recipients``: The receiver list (a list of recipient email addresses).

    - ``sender``: The sender email address to use in sent mails (From).

    - ``severityFilter``: A regular expression used to filter alerts. Alerts 
      with a severity (one of ``DEBUG``, ``INFO``, ``NOTICE``, ``WARN``, 
      ``ERROR``, ``FATAL``) that doesn't match the filter expression are 
      suppressed and not sent. Default: ``.*``.

    - ``mailServer``: Connection settings for the SMTP server through which emails 
      are to be sent.

      - ``smtpHost``: SMTP server host name/IP address.

      - ``smtpPort``: SMTP server port. Default is 25.

      - ``authentication``: Optional username/password to authenticate with SMTP
        server. If left out, authentication is disabled.

      - ``useSsl``: Enables/disables the use of SSL for SMTP connections. Default 
        is false (disabled).

  - ``poolUpdatePeriod`` (optional): The time interval (in seconds) between 
    periodical pool size updates. A pool size update may involve terminating 
    termination-due instances and placing new spot requests to replace 
    terminated spot requests. Default: 60.


License
=======
Copyright (C) 2013 Elastisys AB

Licensed under the Apache License, Version 2.0

