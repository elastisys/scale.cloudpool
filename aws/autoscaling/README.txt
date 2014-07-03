Instructions
============
This module produces an all-in-one "server and application" 
executable jar that runs an embedded Jetty HTTP server that publishes 
the cloud adapter REST API endpoint.

To run the server, simply issue the following command:

  java -jar <artifact>.jar [arguments ...]
  
This will start a server running listening on HTTP port 8080 and HTTPS port 
8443. These ports can be changed (run with the "--help" flag to see the list of 
available options).

If you want to control java.util.logging output produced by embedded libraries,
you can override the default (JRE-provided) log configuration with the 
logging.properties file in this directory. This is done by running: 
 
   java -Djava.util.logging.config.file=./logging.properties -jar <artifact>.jar [arguments ...]

 
