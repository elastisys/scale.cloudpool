scale.cloudadapters
===================



The ``scale.cloudadapters`` project contains Java-based 
[elastisys:scale](http://elastisys.com/scale) 
[cloud adapter](http://cloudadapterapi.readthedocs.org/en/latest) implementations 
for different cloud providers:

  - ``cloudadapers.openstack``: a [cloud adapter](http://cloudadapterapi.readthedocs.org/en/latest) 
    that manages a group of [OpenStack](https://www.openstack.org/) servers.
  - ``cloudadapers.aws.ec2``: a [cloud adapter](http://cloudadapterapi.readthedocs.org/en/latest) 
    that manages a group of [AWS EC2](http://aws.amazon.com/ec2/) instances.
  - ``cloudadapers.aws.autoscaling``: a [cloud adapter](http://cloudadapterapi.readthedocs.org/en/latest) 
    that manages the size of an [AWS Auto Scaling Group](http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/WorkingWithASG.html).


Getting started
===============

Clone the repository and build it with:

    `mvn clean install`


For each of the cloud adapter implementation modules, the build produces an 
executable jar file that starts an HTTP(S) server that publishes the cloud
adapter [REST API](http://cloudadapterapi.readthedocs.org/en/latest/api.html).

To start a server, simply execute the jar file:

  `java -jar <artifact>.jar --https-port 8443 --config <JSON file>`
  
This will start a server running listening on HTTPS port 8443.
The port number can be changed (run with the ``--help`` flag to see the list of 
available options).

The behavior of the cloud adapter is controlled through a JSON-formatted 
configuration document and can either be set at start-up time with the
``--config`` command-line flag, or over a REST API in case the server
has been started with the ``--config-handler`` command-line option.
A configuration can be set over the REST API by a ``POST`` of the document 
to  the ``/config`` endpoint.

The JSON document that is passed as configuration is outlined below.


Configuration
=============
The configuration document follows the same schema for all cloud adapters, 
except for one part that holds cloud provider-specific settings. This part 
of the configuration is stored in the ``scalingGroup`` section under the
``config`` key. This configuration key typically contains credentials needed
to connect to the cloud provider API. The exact properties that each specific
cloud adapter implementation expects can be found in their JSON schema under
``src/main/resources``. Sample configuration documents for each cloud adapter
can typically be found under ``src/test/resources``.

Below is a sample configuration document for the ``cloudadapers.aws.ec2``:


  {
    "scalingGroup": {
      "name": "MyEc2InstanceScalingGroup",
      "config": {
        "awsAccessKeyId": "AXZ31...Q",
        "awsSecretAccessKey": "afAC/3Dd...s",
        "region": "eu-west-1"
      }
    },
    "scaleUpConfig": {
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
    "scaleDownConfig": {
      "victimSelectionPolicy": "CLOSEST_TO_INSTANCE_HOUR",
      "instanceHourMargin": 300
    },
    "liveness": {
      "loginUser": "ubuntu",
      "loginKey": "/path/to/ec2/instancekey.pem",
      "bootTimeCheck": {
        "command": "sudo service apache2 status | grep 'is running'",
        "retryDelay": 20,
        "maxRetries": 15
      },
      "runTimeCheck": {
        "command": "sudo service apache2 status | grep 'is running'",
        "period": 60,
        "maxRetries": 3,
        "retryDelay": 10
      }
    },
    "alerts": {
      "subject": "[elastisys:scale] scaling group alert for MySpotScalingGroup",
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


The configuration document declares how the cloud adapter:

  - should configure its cloud-specific `ScalingGroup` class to allow it to communicate with its cloud API (the ``scalingGroup`` key).

  - provisions new instances when the scaling group needs to grow (the ``scaleUpConfig`` key).

  - decommissions instances when the scaling group needs to shrink (the ``scaleDownConfig`` key).

  - performs *boot-time liveness checks* when starting new group members (the ``bootTimeCheck`` key).

  - performs periodical *run-time liveness checks* on existing group members (the ``runTimecheck`` key).

  - alerts system administrators (via email) when resize operations, liveness checks, etc fail (the ``alerts`` key).



In a little more detail, the configuration keys have the following meaning:

  - ``scalingGroup``: Describes how to identify/manage scaling group members 
    and connect to the cloud provider.

    - ``name``: The logical name of the scaling group. The exact way of 
      identifying group members may differ between implementations, but 
      instance tags could, for example, be used.

    - ``config``: `ScalingGroup`-specific JSON configuration document, the 
      contents of which depends on the particular ScalingGroup-implementation 
      being used. Typically, a minimum amount of configuration includes login 
      credentials for connecting to the particular cloud API endpoint. The 
      properties expected by a certain cloud adapter implementation can be
      found in their JSON schema under ``src/main/resources``.

  - ``scaleUpConfig``: Describes how to provision additional servers (on scale-up).

    - ``size``: The name of the server type to launch. For example, ``m1.medium``.

    - ``image``: The name of the machine image used to boot new servers.

    - ``keyPair``: The name of the key pair to use for new machine instances.

    - ``securityGroups``: The security group(s) to use for new machine instances.

    - ``bootScript``: The script to run after first boot of a new instance.

  - ``scaleDownConfig``: Describes how to decommission servers (on scale-down).

    - ``victimSelectionPolicy``: Policy for selecting which spot instance to 
      terminate. Allowed values: ``NEWEST_INSTANCE``, ``OLDEST_INSTANCE``, 
      ``CLOSEST_TO_INSTANCE_HOUR``.

    - ``instanceHourMargin``: How many seconds prior to the next instance hour 
      an acquired machine instance should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-down is ordered.

  - ``liveness``: Configuration that determines how to monitor the liveness 
    of scaling group members.
  
    - ``sshPort``: The SSH port to connect to on machines in the scaling group. 
      Defaults to 22.

    - ``loginUser``: The user name to use (together with the 'loginKey' when 
      logging in remotely (over SSH) against machines in the scaling group.
 
    - ``loginKey``: The path to the private key file of the key pair used to 
      launch new machine instances in the Auto Scaling group. This key is used
      to log in remotely (over SSH) against machines in the scaling group.

    - ``bootTimeCheck``: Configuration for boot-time liveness tests, which wait
      for a server to come live when a new server is provisioned in the scaling 
      group.
  
      - ``command``: The command/script (executed over SSH) used to determine 
        when a booting machine is up and running. A machine instance is 
        considered live when the command is successful (zero exit code).

      - ``maxRetries``: The maximum number of attempts to run the liveness test 
        before failing.

      - ``retryDelay``: The delay (in seconds) between two successive liveness 
        command retries.


    - ``runTimeCheck``: Configuration for run-time liveness tests, which are 
      performed periodically to verify that scaling group members are still 
      operational.

      - ``period``: The time (in seconds) between two successive liveness test runs.
  
      - ``command``: The command/script (executed over SSH) used to periodically 
        verify that running servers in the pool are still up and running. A 
        machine instance is considered live when the command is successful (zero 
        exit code).

      - ``maxRetries``: The maximum number of attempts to run the liveness test 
        before failing. 

      - ``retryDelay``: The delay (in seconds) between two successive liveness 
        command retries.

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

