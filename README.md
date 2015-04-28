# Docker/Scala Experiments

### Akka Remoting Application
This branch shows a simple remoting app w/Docker+Akka.  The idea is to run an Akka server inside a Docker container and be able to access it outside the container.  For this mode to work, however, you must know and specify the IP of the host this Docker is running on.  If you don't know it (e.g. if this is running in some aggregator like Mesos then this won't work).

To run the server:
```sh
docker run -e "INST_NAME=Fred" -e "HOST_IP=`./getMyIP.sh`" --rm=true -p 1600:2551 -p 8080:8080 quay.io/gzoller/root
```
The INST_NAME variable is just a label for display purposes of this sample application.  Nothing special about it.

In order for Akka remoting to work it has to know its actual IP address, which is problematic in Docker.  Inside the container there's no way to get this information (it'll have its own IP address, which is useless for this).  We must pass in the host's IP address.  We do this with the --add-host argument, using a script (or any Unix command) that returns the host's IP address.  This will insert a record into Docker's /etc/hosts under the name 'dockerhost' (don't change this!).

(For AWS, this *might* work: wget -qO- http://instance-data/latest/meta-data/public-ipv4)

The -e argument accomplishes the same thing a different way -- you don't need both -add-host and -e.  One way or t'other you need to obtain and pass in the host's IP address as this cannot be introspected from inside the Docker instance.

You can also pass -e "HOST_PORT=9100" to map the port for the Akka server.  This is port 2551 inside Docker but is mapped with the -p parameter for docker run.  The HOST_PORT's value must match the mapped value given for -p (or 9100 in this example).

Configured this way you can curl the HTTP /ping endpoint on <host_ip>:8080 and send Akka messages ("hey" in this code) to <host_ip>:9100 (akka.tcp://dockerexp@<host_ip>:1600/user/dockerexp)

All these pieces work together to feed Akka 2.4's new dual-binding scheme, which is key to making this all work inside Docker.  In application.conf the secret sauce is this phrase:

		netty.tcp {
			# Internal Docker
			bind-hostname = ${ip}  # This must NOT be localhost/127.0.0.1!  Set this value in code to internal IP.
			bind-port     = 2551

			# External Docker addr
			hostname = ${settings.ip}
			port     = ${settings.port}
		}

The hostname/port is the external (to Docker) binding.  This gets set to whatever the dockerhost IP is plus the port you pass in with optional env variable HOST_PORT (default 1600).  The bind- variants are IP/port internal to Docker, which basically you only care about if you're running other servers inside the same Docker.
