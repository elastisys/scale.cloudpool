# Cloudpools
A _cloudpool_ is a an abstract, cloud-neutral, management interface for an 
elastic pool of servers. 

Different cloudpool implementations are available, each handling the
communication with its particular cloud provider and the API it offers,
thereby shielding the cloudpool client from such details, allowing 
identical management of server groups for any cloud. 
No matter which cloud infrastructure the group is deployed on,
increasing or decreasing the size of the group is as easy as setting 
a desired size for the group. If the desired size is increased, a new 
machine instance is provisioned. If the desired size is decreased, 
a machine instance is selected for termination (note that, depending
on the cloud and cloudpool implementation, the machine may not be
immediately terminated, but may be kept around as long as possible
until it's about to enter a new billing period). If a machine instance in 
the group is no longer operational, a replacement is provisioned.

A cloudpool offers a number of management primitives for the group of servers
it mananages, the most important ones being primitives for

- Tracking the machine pool members and their states.
- Setting the _desired size_ of the machine pool. The cloud pool 
  continuously starts/stops machine instances so that the number of
  machines in the pool matches the desired size set for the pool.

The cloudpool is managed via a REST API. For all the details, refer
to http://cloudpoolrestapi.readthedocs.io/.

Typically, each cloudpool is used to manage a group of similar servers,
fulfilling one single role in the overall system. Examples include
user-facing web server frontends, application servers, and database read-replicas.
One can think of a single cloudpool as managing the number of replicas of 
a certain micro service.

With Elastisys cloudpools, managing large deployments of micro services,
even across different clouds, becomes easier, more robust, and more 
cost-efficient.


## Implementations
The growing list of cloudpool implementations includes. For more details on 
each implementation and its use, refer to its `README.md`:

  - `ec2pool`: a cloudpool that manages a group of [AWS EC2](http://aws.amazon.com/ec2/) instances. 
    [README](aws/ec2/README.md)
  - `spotpool`: a cloudpool that manages a group of [AWS Spot](http://aws.amazon.com/ec2/spot/) instances.
    [README](aws/spot/README.md)
  - `awsaspool`: a cloudpool that manages an [AWS Auto Scaling Group](http://docs.aws.amazon.com/AutoScaling/latest/DeveloperGuide/WorkingWithASG.html). 
    [README](aws/autoscaling/README.md)
  - `citycloudpool`: a cloudpool that manages a group of [OpenStack](https://www.openstack.org/) servers in [City Cloud](https://www.citycloud.com/). 
    [README](citycloud/README.md)
  - `azurepool`: a cloudpool that manages a group of [Microsoft Azure](https://azure.microsoft.com/en-us/) VMs. 
    [README](azure/README.md)
  - `gcepool`: a cloudpool that manages a [GCE instance group](https://cloud.google.com/compute/docs/instance-groups/#managed_instance_groups).
    [README](google/compute/README.md)
  - `gkepool`: a cloudpool that manages a [GKE container cluster](https://cloud.google.com/container-engine/docs/clusters/).
    [README](google/container/README.md)
  - `kubernetespool`: a cloudpool that manages a group of [Kubernetes](https://kubernetes.io/) pod replicas. [README](kubernetes/README.md)
  - `openstackpool`: a cloudpool that manages a group of [OpenStack](https://www.openstack.org/) servers.
    [README](openstack/README.md)
	

For implementers, it may be worth noting that the [cloudpool.commons](commons) 
module contains a generic `CloudPool` implementation (`BaseCloudPool`) intended
to be used as a basis for building cloud-specific cloudpools.

A `MultiCloudPool`, which allows a dynamic collection of *cloudpool instances* 
to be published on a single server, is also available under the 
[multipool](multipool/README.md) module. All of the above cloudpool 
implementations are possible to run both as singleton cloudpools and 
as multipools.


## Building

This project depends on [scale.commons](https://github.com/elastisys/scale.commons).
If you are building from `master` yourself (where the `pom.xml` file refers to 
a `SNAPSHOT` version), you need to clone and build that code repository first.

Once that has been installed using Maven, this project can be built with:

  `mvn clean install`


For each of the cloudpool implementation modules, the build produces an 
executable jar file that starts an HTTP(S) server that publishes the cloud
pool [REST API](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html).

To start a server, simply execute the jar file (in the `target` directory of
the cloudpool implementation's module):

  `java -jar <artifact>.jar ...`
  
This will start a HTTP/HTTPS server publishing the 
[cloudpool REST API](http://cloudpoolrestapi.readthedocs.org/en/latest/api.html)
at the specified port. For a full list of options run with the `--help` flag.

The behavior of the cloudpool is controlled through a JSON-formatted 
configuration document and can either be set at start-time with the
`--config` command-line flag, or over the 
[POST /config](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#set-configuration) 
REST API method.

The JSON document is specific to the cloudpool implementation (refer to 
its `README.md` for full details) but some common configuration options 
are described  below.


## Configuration
_Most_ of the cloudpool implementations follow a similar schema for 
the configuration document (refer to the individual cloudpool's `README.md`
for details). In the general schema, outlined below, there are two parts 
of the configuration document that carries cloud provider-specific settings:

- `cloudApiSettings`: typically declares API access credentials and settings.
- `provisioningTemplate`: contains cloud provider-specific server provisioning parameters.
  
The configuration parameters supported by a particular cloudpool implementation can be
found in its `README.md` file.

A common structure of a cloudpool configuration is illustrated below:

```javascript
{
    "name": "webserver-pool",
	
	"cloudApiSettings": {
        ... cloud provider-specific API access credentials and settings ...
    },
	
    "provisioningTemplate": {
        ... cloud provider-specific provisioning parameters ...
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

The configuration document declares how the cloudpool:

  - identifies pool members (the `name` key). As an example, the cloudpool
    implementation may choose to assign a metadata tag with the pool name to 
	each started machine.

  - should configure its cloud-specific [CloudPoolDriver](commons/src/main/java/com/elastisys/scale/cloudpool/commons/basepool/driver/CloudPoolDriver.java) to allow it to communicate with its cloud API (the `cloudApiSettings` key).

  - provisions new machines when the pool needs to grow (the `provisioningTemplate` key).

  - decommissions machines when the pool needs to shrink (the `scaleInConfig` key).

  - alerts system administrators (via email) of resize operations, error conditions, etc (the `alerts` key).


In a little more detail, the configuration keys have the following meaning:

  - `name`:  (**required**): The logical name of the managed group of machines. 
    The exact way of identifying pool members may differ between implementations, but 
    machine tags could, for example, be used to mark pool membership.
	  
  - `cloudApiSettings` (**required**): API access credentials and settings required to
    communicate with the targeted cloud. The structure of this documnent is 
	cloud-specific. Refer to the `README.md` of a particular cloud implementation for
	details.
	
  - `provisioningTemplate` (**required**): Describes how to provision additional
    servers (on scale-out). The appearance of this document is cloud-specific. 
	Refer to the `README.md` of a particular cloud implementation for details.

  - `scaleInConfig` (*optional*): Describes how to decommission servers (on scale-in).  
    Default scale-in policy is to immediately terminate newest instance.

    - `victimSelectionPolicy` (**required**): Policy for selecting which machine to 
      terminate. Allowed values: `NEWEST_INSTANCE`, `OLDEST_INSTANCE`, 
      `CLOSEST_TO_INSTANCE_HOUR`.

    - `instanceHourMargin` (**required**): How many seconds prior to the next instance 
	  hour an acquired machine should be scheduled for termination. This 
      should be set to a conservative and safe value to prevent the machine 
      from being billed for an additional hour. A value of zero is used to 
      specify immediate termination when a scale-in is ordered.

  - `alerts` (*optional*): Configuration that describes how to send alerts via email or HTTP(S) webhooks.
    - `duplicateSuppression` (optional): Duration of time to suppress
      duplicate alerts from being re-sent. Two alerts are considered equal if
      they share topic, message and metadata tags.
    - `smtp`: a list of email alert senders
      - `subject`: The subject line to use in sent mails (Subject).
      - `recipients`: The receiver list (a list of recipient email addresses).
      - `sender`: The sender email address to use in sent mails (From).
      - `severityFilter`: A regular expression used to filter alerts. Alerts 
        with a severity (one of `DEBUG`, `INFO`, `NOTICE`, `WARN`, 
        `ERROR`, `FATAL`) that doesn't match the filter expression are 
        suppressed and not sent. Default: `.*`.
        - `smtpClientConfig`: Connection settings for the SMTP client.
          - `smtpHost`: SMTP server host name/IP address.
          - `smtpPort`: SMTP server port. Default is 25.
          - `authentication`: Optional username/password to authenticate with SMTP
            server. If left out, authentication is disabled.
          - `useSsl`: Enables/disables the use of SSL for SMTP connections. Default 
            is false (disabled).
    - `http`: a list of HTTP(S) webhook alert senders, which will `POST` alerts
       to the specified endpoint using the (optional) configured authentication 
       credentials.
      - `destinationUrls`: The list of destination endpoint URLs.
      - `severityFilter`: A regular expression used to filter alerts. Alerts 
        with a severity (one of `DEBUG`, `INFO`, `NOTICE`, `WARN`, 
        `ERROR`, `FATAL`) that doesn't match the filter expression are 
        suppressed and not sent. Default: `.*`.
      - `auth`: Authentication credentials. Can specify either `basicCredentials`
        or `certificateCredentials` or both.
        - `basicCredentials`: `username` and `password` to use for BASIC-style
          authentication.
        - `certificateCredentials`: `keystorePath` and `keystorePassword`
          for client certificate-based authentication.
  - `poolFetch` (*optional*): Controls how often to refresh the cloud 
    pool member list and for how long to mask cloud API errors. 
    Default: `retries`: 3 retries with 3 second initial exponential back-off delay,
    `refreshInterval`: 30 seconds, `reachabilityTimeout`: 5 minutes.
    - `retries`: Retry handling when fetching pool members from the cloud API fails.
      - `maxRetries`: Maximum number of retries to make on each attempt to fetch pool 
        members.
      - `initialBackoffDelay`: Initial delay to use in exponential back-off on retries. 
        May be zero, which results in no delay between retries.
    - `refreshInterval`: How often to refresh the cloudpool's view of the machine 
      pool members.
    - `reachabilityTimeout`: How long to respond with cached machine pool observations
      before responding with a cloud reachability error. In other words, for how long should
      failures to fetch the machine pool be masked.
  - `poolUpdate` (*optional*): Controls the behavior with respect to how often to 
    attempt to update the size of the machine pool to match the desired size.
    - `updateInterval`: The time interval between  periodical pool size updates. 
      Default: 60 seconds.


## Multi-cloud support

Elastisys has also developed a Splitter cloudpool implementation, which lets
a single logical cloudpool span across several clouds (and even cloud providers),
complete with fail-over functionality built in. It adheres to the exact same 
[cloudpool API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html).
Users of the a cloudpool defines a splitting policy, such as 
"90 percent AWS Spot instances, 10 percent AWS EC2 instances", and the 
Splitter cloudpool takes care of maintaining this ratio.

Should a cloud fail to provide an instance fast enough (for whatever reason), 
the Splitter cloudpool will obtain an equivalent instance from another of 
its configured cloud backends. Once the original cloud provider is operating
as intended again, the Splitter will automatically decommission the replacement
machine from the other cloud backend.

Some of our customers use it to ensure that their services are highly available, 
even in the face of cloud provider failure. Others use it to run mostly Spot
instances, and fall back to on-demand instances when Spot instances are scarce.

Read more about it on our website: https://elastisys.com/cloud-platform-features/multi-cloud/

The Splitter cloudpool is not open source at this time, so 
[contact Elastisys](http://elastisys.com/contact/) if you would like to
discuss how the Splitter cloudpool can help optimize your cloud deployment.

License
=======
Copyright (C) 2013 Elastisys AB

Licensed under the Apache License, Version 2.0

