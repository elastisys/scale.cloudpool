## cloudpool.commons module

The `cloudpool.commons` module provides basic Java building blocks for 
implementing [elastisys](http://elastisys.com/) 
[cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest) implementations 
for different cloud providers.


This module provides a generic [CloudPool](../api/src/main/java/com/elastisys/scale/cloudpool/api/CloudPool.java) that is provided as a basis for building
cloud-specific pools. This generic [BaseCloudPool](src/main/java/com/elastisys/scale/cloudpool/commons/basepool/BaseCloudPool.java) implements sensible behavior for the `CloudPool`
methods and relieves implementors from dealing with the details of continuously 
monitoring and re-scaling the pool with the right amount of machines given the member 
machine states, handling (re-)configurations, sending alerts, etc. 

Implementers of cloud-specific `CloudPool`s only need to implements a small set of 
machine pool management primitives for a particular cloud. These management primitives 
are supplied to the `BaseCloudPool` at construction-time in the form of a
[CloudPoolDriver](src/main/java/com/elastisys/scale/cloudpool/commons/basepool/driver/CloudPoolDriver.java), which implements the management primitives according to the API of 
the targeted cloud.

Cloud-specific configuration is passed to the `CloudPoolDriver` via placeholders in
the [BaseCloudPool configuration](../README.md#configuration) document.
