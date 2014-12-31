# Docker/Scala Experiments

This project explores how to build and deplay Docker containers with Akka for use across multiple nodes.  Other examples exist showing Docker+Akka but seem to use linking, which won't work across multiple host instances.

My solutions rely on some new functionality in Akka 2.4 (Snapshot release available at time of this writing).  Since 2.4 also includes the first cut of Akka's incorporation of Spray (now Akka HTTP), these examples use that approach.

### Branches
This project has 3 branches showing different uses of Akka/HTTP with Docker.

* http -- Simple 'ping' HTTP service
* remoting -- Akka remoting
* cluster -- Akka clustering

### Mac
Docker is Linux-specific (assumes Linux) so on a Mac we need to run a virtual machine.  Fortunately you can just use boot2docker, which is basically preconfigured for Docker.  In a boot2docker shell, install docker with apt-get.  Then we need to run a local Docker repo, so pull down a local repo image and run it:

```sh
docker pull samalba/docker-registry
docker run -d -p 5000:5000 samalba/docker-registry
```

Then you'll need to map your Mac's ports to the VM with this:

```sh
VBoxManage controlvm boot2docker-vm natpf1 "name,tcp,127.0.0.1,1234,,1234"
```
(Where 1234 is the port you want to open.)  Boot2Docker installs a copy of VirtualBox so it may be easier for you to add the ports you want to expose through the network configuration there.

###Cleanup
Sometimes Docker images gets "stuck" and you can't delete it, even after everything is stopped.  Try this, then re-try your delete:

```sh
docker rm `docker ps -a -q`
```
