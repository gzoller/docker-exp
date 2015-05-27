# Working with AWS

### Amazon and the Ocean
The normal process of running a Docker-ized Akka application as described will work fine on EC2.  However in AWS we can (and we need) better!  Running individual containers is no problem, but AWS has a new container service, ECS.  I call this an "ocean" into which we launch Docker containers.

The challenge with oceans is that they like to handle the port mappings we usually do manually with -p arguments.  This is because ECS may run &gt;1 instance of a container on the same machine so it needs control of port allocation.  Great for ECS--bad for Akka.

You'll notice in the manual arrangement that we need to pass in things like host IP and port.  How will we get this information in ECS?  It's not easy but it is possible!  The secret is to utilize all the introspection facilities available in Docker and EC2 to our advantage.  These are EC2-specific so they will not work in a non-Amazon environment.

### The Plan
To make the magic work we need to automatically figure out the host IP and the port from within Docker.  Remember, in an "ocean" environment we won't be able to pass this information in like we do when run manually!

We have 2 primary sources for introspection information:

 1. Docker's RESTful endpoints for information
 2. AWS's metadata service

AWS's metadata can provide lots of nice information, and the host IPs (yes, there are more than 1) are of most interest to us.  Docker's information can tell us port mappings if we know the id of the running container (i.e. this Docker), so ultimately that's where the port information comes from.

Let's get the easy information first: the host's IP.  From inside the Docker we can call AWS's metadata service to get this information.  There are actually 2 IPs we need but let's deal with the simple one first: the public IP for the dual binding.  We obtain this by a GET call to http://169.254.169.254/latest/meta-data/public-ipv4.  That was easy!  This IP gets assigned in the Scala code (over-writes, actually) the bind-hostname parameter in application.conf.  This IP is the one that we use to build the Akka URI for other nodes outside this Docker to call this node.

Next we figure out the port mapping to get the externally mapped port for Akka.  This is more complex.  First we need to get the id of this running Docker container.  Conveniently (in EC2 at least) this is pre-assigned to the HOSTNAME environment variable inside the Docker.

Next we need to get information about all the running containers on this host.  Docker has this information bound to a file endpoint but in the setup section below I show how to bind it to port 5555 so we can hit it like a normal RESTful endpoint.  We're so close now, but there's a problem.

What IP do we use to hit port 5555?  We could try the host's IP we got earlier.  Well, it turns out that's the "public" IP visible outside AWS.  It's not visible/useful inside the AWS cloud.  There's a "local" IP needed for that.  We obtain the host's local IP with a GET call to http://169.254.169.254/latest/meta-data/local-ipv4.  With this IP we GET port :5555/containers/json to obtain information about all running containers.  The output from GET to port 5555 looks something like this:

    [
      {
        "Command": "bin/root",
        "Created": 1432418467,
        "Id": "743a9a256262e427e8d630c785dc0f7d3016e06fed94baf6988691a89198c642",
        "Image": "quay.io/gzoller/root:latest",
        "Labels": {},
        "Names": [
          "/serene_leakey"
        ],
        "Ports": [
          {
            "IP": "0.0.0.0",
            "PrivatePort": 8080,
            "PublicPort": 9100,
            "Type": "tcp"
          },
          {
            "IP": "0.0.0.0",
            "PrivatePort": 2551,
            "PublicPort": 9101,
            "Type": "tcp"
          }
        ],
        "Status": "Up Less than a second"
      }
    ]

From here we filter the container info list on the id (HOSTNAME) and look up the port mapping for port 2551 (default).  This is the port we'll use in the Akka URI for this node.

Now we have everything: the host's IP and the port.

Lots of moving pieces but it all does work!

### Setup

 1. Get a public Docker repository or some other place to host your Docker images.  I opted to use quay.io.  Accounts for public repos are free.  In Build.scala I configured this with:
	  ```dockerRepository := Some("quay.io/gzoller")```

 2. Be sure your instances in AWS have security policies that allow for access on ports your want to experiment with; 9100 and 9101 (for this Docker demo) for Akka and HTTP respectively for our purposes here.
 3. Add the following to the /etc/sysconfig/docker file on your target EC2 instance(s)
	 ```OPTIONS="-H 0.0.0.0:5555 -H unix:///var/run/docker.sock"```
	 This will bind Docker's introspection information to port 5555 as a REST service.  **WARNING: This is insecure on an open network so be sure port 5555 is only accessible locally!**
	 
 4. Build and publish your Docker image from sbt with 
	 ```docker:publish```

### Running
Log into your AWS instance (which should already have Docker installed) and follow these steps:

```
sudo docker run -d -p 9101:2551 -p 9100:8080 quay.io/gzoller/root
```

Where quay.io/gzoller is the name of your Docker hosted in Quay.  You can now access the public IP of this instance and hit port &lt;ip&gt;:9100/ping for HTTP access and send message "hey" using Akka to &lt;ip&gt;:9101

