# Working with AWS

### Amazon and the Ocean
The normal, manual process of running a Docker-ized Akka application as described before will work fine on EC2.  However in AWS we can do much better!  Within EC2 there are facilities available that allow us to introspect the environment to get the host IP and running ports.

AWS has a new container service, ECS, for automatically running and managing Docker containres.  I call this an "ocean" -- something into which we launch Docker containers.

The challenge with oceans is that they need to handle the port mappings for you (the ports we usually map manually with -p arguments).  This is because ECS may run &gt;1 instance of a container on the same machine so it needs control of port allocation.  Great for ECS--bad for Akka.

You'll notice in the manual arrangement that we need to pass in the host IP and port for Akka's dual-binding.  How will we get this information in ECS?  We need to somehow obtain this information from *within a running Docker*.  The secret is to utilize all the introspection facilities available in Docker and EC2 to our advantage.  These are EC2-specific so they will not work in a non-Amazon environment.

###Preparation
To make the magic work we need to automatically figure out the host IP and the port from within Docker.  Remember, in an "ocean" environment we won't be able to pass this information in like we do when run manually.

Docker has a RESTful facility built into its agent containing very useful information.  It's exposed on a file system that isn't very user-friendly out-of-the-box.  I prefer to map this to a port so it can be accessed via HTTP, as shown in the steps below.  

 1. Get a Docker repository to host your Docker images.  I opted to use quay.io.  In Build.scala I configured this with:
    ```dockerRepository := Some("quay.io/gzoller")```

 2. Be sure your instances in AWS have security group policies that allow for access on ports your want to experiment with; 9100 and 9101 (for this Docker demo) for Akka and HTTP respectively.
 3. Create a new EC2 instance with Docker installed and add the following to the /etc/sysconfig/docker file on your target EC2 instance(s)
   ```OPTIONS="-H 0.0.0.0:5555 -H unix:///var/run/docker.sock"```
This will bind Docker's introspection information to port 5555 as a REST service.  **WARNING: This is insecure on an open network so be sure port 5555 is only accessible on this host!**
 4. You may want to create a snapshot of this EC2 instance with this modification to use as a ready  template for creating other Docker-ized instances.
 5. Build and publish your Docker image from sbt with docker:publish

### The Plan
AWS's metadata can provide lots of nice information.  The host IPs (yes, there are more than 1) are of most interest to us.  Docker's RESTful information can tell us port mappings if we know the id of the running container (i.e. this Docker), so ultimately that's where the port information comes from.

First let's think about the host IP.  There are two: a local and a public IP.  The local IP is visible within AWS and the public IP is visible from outside AWS.  We need to use the right IP depending on what we need.

The Scala code in the example uses these calls to determine the two host IPs from AWS:

http://169.254.169.254/latest/meta-data/local-ipv4
http://169.254.169.254/latest/meta-data/public-ipv4

The code will use one of these IPs to bind to Akka, but which?  

If you want an Akka node running in a Docker container to be visible only from within AWS (i.e. Akka-to-Akka communication only inside AWS) then the local IP will be used.  If you need to send Akka messages from outside AWS use the public IP.  The local IP is used by default.  To use the public IP add the following to 'docker run':

    -e EXT_AKKA=true

Next we need the port mapping to get the mapped port for Akka running inside the Docker container.  We will need to get the id of this running Docker container.  Conveniently (in EC2 at least) this is pre-assigned to the HOSTNAME environment variable inside the Docker.  Using this id we'll use our port 5555 service set up earlier to do this.

Using the local IP of the host we GET port :5555/containers/json to obtain information about all running containers.  The output from GET to port 5555 looks something like this:

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

Filter the container info list on the id (HOSTNAME) and look up the port mapping for port 2551 (Akka default).  This is the port we'll use in the Akka URI for this node (9101 in this example, but will be whatever ECS uses when running in the ocean).

Now we have everything: the host's IP and the port.

Lots of moving pieces but it all works.

### Running
Log into your AWS instance (which should already have Docker installed) and follow these steps:

```
sudo docker run -d -p 9101:2551 -p 9100:8080 -e EXT_AKKA=true quay.io/gzoller/root
```

Where quay.io/gzoller would be the name of your Docker image.  You can now access the public IP of this instance and hit port &lt;ip&gt;:9100/ping for HTTP access and send message "hey" using Akka to &lt;ip&gt;:9101


