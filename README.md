# direct-james-server
The direct james server is an reassembly of the Apache James mail server as a SpringBoot application.  Although it can be modified, the default configuration is tailored to use the server as an edge protocol server (not as an SMTP server for receiving messages from other HISPs or as a relay) and to store messages until they are picked up by and edge client.  

It also exposes a RESTFul web interface (protected by basic auth by default) to configure aspects of the server.  The web administration API is documented by the James project web admin [page](https://james.apache.org/server/manage-webadmin.html).  Domains and users can be added via this interface.
