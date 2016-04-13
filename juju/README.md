# juju cloud pool

This is a basic [cloud pool](http://cloudpoolrestapi.readthedocs.org/en/latest/) implementation
for Canonical's [Juju system](https://github.com/juju/juju/).  

Canonical has not yet released a Java API, so until they do, we use the official command line
client for interacting with the Juju system. It **must** be installed for this cloud pool to work.
If you use the Docker container that this project builds, this requirement is automatically
fulfilled. If you want to use the Java JAR artifact on its own, please follow the [installation
instructions carefully](https://jujucharms.com/docs/stable/getting-started) to ensure that you
have a working setup.

