# Docker/Scala Experiments

### Akka Remoting Application
This branch shows a simple remoting app w/Docker+Akka.  The idea is to run an Akka server inside a Docker container and be able to access it outside the container.  

To run the server:
```sh
docker run -d -p 9100:2551 -p 9101:8080 --rm="true" --name dexp localhost:5000/root --name Fred --hostIP 192.168.0.2 --hostPort 9100
```
The first --name is the Docker container name.  The second --name (after localhost:5000/root) is the name of the Akka server name.

--hostIP is the local host machine's IP address

--hostPort is the mapped port for the Akka server.  This is port 2551 inside Docker but is mapped with the -p parameter for docker run.  The --hostPort value must match the mapped value given for -p (or 9100 in this example).

Configured this way you can curl the HTTP /ping endpoint on 192.168.0.2:9101 and send Akka messages ("hey" in this code) to 192.168.0.2:9100.