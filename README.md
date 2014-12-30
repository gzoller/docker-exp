# Docker/Scala Experiments

##NOTE: This branch gets simple remoting + HTTP going
Run with: docker run -p 9100:9100 -p 9101:9101 -i -t localhost:5000/root --name Greg --hostIP 10.0.0.125 --hostPort 9100 --httpPort 9101

A simple project to experiment with how to create Docker images using sbt with related plugins. The 'ping' branch is a simple /ping endpoint.  The /akka branch shows how to put an Akka cluster in a Docker container.

To run the server:
```sh
docker run -d -p 9090:9090 --name dexp localhost:5000/root:1.0.0 bin/root
```

And to join the running process to look around:
```sh
docker exec -i -t dexp /bin/bash
```

### Mac
Docker is Linux-specific (assumes Linux) so on a Mac we need to run a virtual machine.  Fortunately you can just use boot2docker.  In a boot2docker shell, install docker with apt-get.  Then pull down a local repo image and run it:

```sh
docker pull samalba/docker-registry
docker run -d -p 5000:5000 samalba/docker-registry
```

Then you'll need to map your Mac's ports to the VM with this:

```sh
VBoxManage controlvm boot2docker-vm natpf1 "name,tcp,127.0.0.1,1234,,1234"
```
(where 1234 is the port you want to open.  Do this for port 9090 for this experiment.)

###Cleanup
Sometimes a Docker image gets "stuck" and you can't delete it.  Try this, then re-try the delete:

```sh
docker rm `docker ps -a -q`
```
