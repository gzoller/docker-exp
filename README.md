# Docker/Scala Experiments

### Akka Remoting Application
This branch shows a simple Akka remoting app w/Docker.  The idea is to run an Akka server inside a Docker container and be able to access it outside the container.  In principle this should be easy but in practice has some interesting complexities driven by Docker's encapsulation of networking.

From Akka's perspective, Docker networking is basically a NAT, which it doesn't handle--at least not until version 2.4.  Akka 2.4 provides "dual binding" where you can bind to a host/ip inside a Docker container and also bind to the external host/ip outside (i.e. the Docker's host).

This sample uses 2.4's latest support for dual-binding.  To build the example modify the in Build.scala to point to your own account/repo and do:

```sh
sbt docker:publish
```

To run the server on your target Docker-enabled environment:
```sh
sudo docker run --rm=true -e "HOST_IP=192.168.0.1" -e "HOST_PORT=9101" -p 9101:2551 -p 9100:8080 quay.io/gzoller/root
```

In order for Akka remoting to work it has to know its actual IP address, which is problematic in Docker.  Inside the container there's no easy way to get this information.  It'll have its own IP address, which is useless for our needs here.  We must pass in the host's IP address.  We do this with the -e argument and either the specific IP of the host.  This will create a HOST_IP environment variable, set to the host's IP, that is visible inside the container.

The same challenges exist for the Akka port.  We map the internal Akka port of 2551 to an externally visible port of 9101 with the -p argument, but just as for the host's IP we must somehow make this information available to Akka running inside the Docker container.  Pass this port into the container as well with the HOST_PORT environment variable created with another -e argument.  *The port value sent in with -e must match the -p value! (9101 in this case)*

Configured this way you can curl the HTTP /ping endpoint on &lt;host_ip&gt;:9100 and send Akka messages ("hey" in this code) to &lt;host_ip&gt;:9101 (akka.tcp://dockerexp@<host_ip>:9101/user/dockerexp)

All these pieces work together to feed Akka 2.4's new dual-binding scheme, which is key to making this all work inside Docker.  In application.conf the secret sauce is this phrase:

		netty.tcp {
			# Internal Docker
			bind-hostname = ${ip}  # This must NOT be localhost/127.0.0.1!  Set this value in code to internal IP.
			bind-port     = 2551

			# External Docker addr
			hostname = ${settings.ip}
			port     = ${settings.port}
		}

The bind- variants are IP/port internal to Docker, which basically you only care about if you're running other servers inside the same Docker, and in that case you would clearly need more ports than 2551.

If you are running on Amazon's EC2 service there are further optimizations possible that make life easier and permit using their container service, ECS.  [See information here.](AWS.md)

