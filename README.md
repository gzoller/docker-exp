# Docker/Scala Experiments

### Akka Remoting Application
This branch shows a simple remoting app w/Docker+Akka.  The idea is to run an Akka server inside a Docker container and be able to access it outside the container.  For this mode to work, however, you must know and specify the IP of the host this Docker is running on.  If you don't know it (e.g. if this is running in some aggregator like Mesos then this won't work).

To run the server:
```sh
docker run -d --add-host=dockerhost:`./getMyIP.sh` -e "HOST_IP=`./getMyIP.sh`" -p 9100:2551 -p 9101:8080 --name dexp localhost:5000/root --name Fred  --hostPort 9100
```
The first --name is the Docker container name.  The second --name (after localhost:5000/root) is just a label used in the server for display purposes.

In order for Akka remoting to work it has to know its actual IP address, which is problematic in Docker.  Inside the container there's no way to get this information (it'll have its own IP address, which is useless for this).  We must pass in the host's IP address.  We do this with the --add-host argument, using a script (or any Unix command) that returns the host's IP address.  This will insert a record into Docker's /etc/hosts under the name 'dockerhost' (don't change this!).

(For AWS, this *might* work: wget -qO- http://instance-data/latest/meta-data/public-ipv4)

The -e argument accomplishes the same thing a different way -- you don't need both -add-host and -e.  One way or t'other you need to obtain and pass in the host's IP address as this cannot be introspected from inside the Docker instance.

--hostPort is the mapped port for the Akka server.  This is port 2551 inside Docker but is mapped with the -p parameter for docker run.  The --hostPort value must match the mapped value given for -p (or 9100 in this example).

Configured this way you can curl the HTTP /ping endpoint on <host_ip>:9101 and send Akka messages ("hey" in this code) to <host_ip>:9100 (akka.tcp://dockerexp@<host_ip>:9100/user/dockerexp)

All these pieces work together to feed Akka 2.4's new dual-binding scheme, which is key to making this all work inside Docker.  In application.conf the secret sauce is this phrase:

		netty.tcp {
			# Internal Docker
			bind-hostname = ${dkr.local}
			bind-port     = 2551

			# External Docker addr
			hostname = ${dkr.hostname}
			port     = ${dkr.port}
		}

The hostname/port is the external (to Docker) binding.  This gets set to whatever the dockerhost IP is plus the port you pass in with  --hostPort.  The bind- variants are IP/port internal to Docker, which basically you only care about if you're running other servers inside the same Docker.
