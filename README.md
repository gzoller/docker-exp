# Docker/Scala Experiments

### HTTP 'ping' Application
A simple project to experiment with how to create Docker images using sbt.  This branch is a simple /ping endpoint.  

To run the server:
```sh
docker run -d -p 9090:9090 --name dexp localhost:5000/root
```

And to join the running process to look around:
```sh
docker exec -i -t dexp /bin/sh
```
You should be able to find output from your host with:

```sh
curl 127.0.0.1:9090/ping
```