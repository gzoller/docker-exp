# Docker/Scala Experiments

### Akka Cluster Application
This is the main event--getting Akka clustering working with Docker.  We want to run Akka servers inside Docker containers, like the remoting example, but this time the clustering magic (like node discovery) has to work.  

The principle magic here is that IP addresses inside a Docker container are internal-only.  They're useless outside the container.  Akka 2.4+ supports a bind-hostname/bind-port, meaning Akka can bind to >1 address.  In our case we'll use that new facility to bind to both the internal Docker IP (which we basically don't care about) and the external host's IP, which we'll pass in as a command-line parameter set when we launch Docker.

You'll want to run 3 servers: 1 seed + 2 nodes for this example.  Node the different names, ports, and roles.

To run the server:
```sh
docker run -p 9100:2551 -p 9101:8080 -d localhost:5000/root --seed --name Fred --hostIP 10.0.0.125 --hostPort 9100 --roles seed

docker run -p 9200:2551 -p 9201:8080 -d localhost:5000/root --name Barney --hostIP 10.0.0.125 --hostPort 9200 --roles "node,n1" 10.0.0.125:9100

docker run -p 9300:2551 -p 9301:8080 -d localhost:5000/root --name Wilma --hostIP 10.0.0.125 --hostPort 9300 --roles "node,n2" 10.0.0.125:9100
```
--hostIP is the local host machine's IP address

--hostPort is the mapped port for the Akka server.  This is port 2551 inside Docker but is mapped with the -p parameter for docker run.  The --hostPort value must match the mapped value given for -p (or 9100 in this example).

You can curl any of these nodes using their exposed/mapped HTTP ports:
```sh
curl 10.0.0.125:9101/ping
curl 10.0.0.125:9201/ping
curl 10.0.0.125:9301/ping
```

You can see what nodes were discovered by looking at one of them:
```sh
curl 10.0.0.125:9201/nodes
```

Finally, you can intentionally send a message to node 1 that will (internally) have to send an Akka message to a remote/clustered actor running on node 2:
```sh
curl 10.0.0.125:9201/wire
```
This talks to node Barney (role n1) and should produce some message referening Wilma (role n2).