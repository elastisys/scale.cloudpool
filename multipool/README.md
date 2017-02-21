## MultiCloudPool
A `MultiCloudPool` is a service that provides access to 
a collection of cloudpool _instances_. New cloudpool instances can be 
created and deleted dynamically. This in contrast to a regular (singleton)
cloudpool server, which only publishes a single cloudpool.

The `MultiCloudpool` provides a REST API which offers cloudpool instance 
management primitives (list/create/delete) as well as access to each 
individual cloudpool instance over the regular 
[cloudpool REST API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html).
More on that below.

At the moment, a `DiskBackedMultiCloudPool` is provided, which stores the 
state of its cloudpool instances to disk, to allow recovery on restart. It
requires a `CloudPoolFactory` implementation that produces cloudpool instances
of a given kind (for example, a `Ec2CloudPoolFactory`). Different 
factory implementations and examples of how to create a `MultiCloudPoolServer`
are available in the different cloudpool implementations (typically in the
`multipool` package of each module).

All cloudpool implementation docker images should be written to support running
in "multipool-mode". To test this, there is a simple script

    test-multipool-docker-images.sh

which, when executed, scans all project modules for cloudpool implementations
that build docker images. For each cloudpool docker image found, it will attempt
to run it in multipool-mode (environment variable `MULTIPOOL=true`) and make 
some basic tests, checking that it is possible to create/list/delete cloudpool
instances of the given kind.


### Cloudpool instance management
The following REST API resources are provided to manage the collection
of cloudpool instances:

**Create cloudpool instance**
- Method: `POST /cloudpools/<name>`
- Description: Creates a new cloudpool instance named `<name>` and adds it 
  to the collection.
  
   The created instance will be in an unconfigured and unstarted state and
   the instance's [API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html) will be available under `/cloudpool/<name>/`.
- Output: on success: a `201` response message with a `Location` header 
  specifying the URL of the created instance. On error: a non-`2XX` 
  response code with an error response message as described in the 
  [cloudpool REST API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message).

**List cloudpool instances**
- Method: `GET /cloudpools`
- Description: Retrieves the URLs of all cloudpool instances in the collection.
- Output: on success: a `200` response message with a JSON array of
  cloudpool instance URLs. On error: a non-`2XX` 
  response code with an error response message as described in the 
  [cloudpool REST API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message).

**Delete cloudpool instance**
- Method: `DELETE /cloudpools/<name>`
- Description: Deletes the cloudpool instance with the given name from the 
  collection.
- Output: on success: a `200` response message. 
  On error: a non-`2XX` response code with an error 
  response message as described in the [cloudpool REST API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message).


### Cloudpool instance access
The full [cloudpool REST API](http://cloudpoolrestapi.readthedocs.io/en/latest/api.html#error-response-message) can be accessed for each cloudpool instance under `/cloudpools/<name>/...`.

For example, to get the configuration of a cloudpool instance named `my-pool`,
call `GET /cloudpools/my-pool/config`. To set a new desired size, `POST /cloudpools/my-pool/pool/size`, and so on ...
