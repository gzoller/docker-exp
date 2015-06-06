# Working with AWS

## Amazon and the Ocean
The normal, manual process of running a Docker-ized Akka application as described before will work fine on EC2.  However in AWS we can do much better!  Within EC2 there are facilities available that allow us to introspect the environment to get the host IP and running ports.

AWS has a new container service, ECS, for automatically running and managing Docker containers.  I call this an "ocean" -- something into which we launch Docker containers without regard to the instances they run on.

The challenge with oceans is that they need to handle the port mappings for you (the ports we usually map manually with -p arguments).  This is because ECS may run &gt;1 instance of a container on the same machine so it needs control of port allocation.  Great for ECS--bad for Akka.

You'll notice in the manual arrangement that we need to pass in the host IP and port for Akka's dual-binding.  How will we get this information in ECS?  We need to somehow obtain this information from *inside* a running Docker.  The secret is to utilize all the introspection facilities available in Docker and EC2 to our advantage.  These are EC2-specific so they will not work in a non-Amazon environment.

##The Idea
We need 2 bits of information to make Akka's dual-binding magic work:  The IP of the host and the port Akka is bound to on the host (outside the docker).  Manually we passed this in but now we need to introspect this information from inside the Docker.

We'll use the AWS metadata service to get the host IP.  Actually we'll obtain 2 IPs: the local IP (useable from within EC2) and the public IP (visible outside EC2--and not visible inside EC2!).  Typically we'd only care about the local IP but for testing, or if you have an application that's building an Akka cluster across providers then this is useful.

Docker has a facility for introspecting all kinds of information about itself and this can be mapped to a port for RESTful access.  Note that this port should be secured via a group or policy so others can't see it!

The key info Docker introspection will give us lives on the /containers/json endpoint.  There we'll find port mappings, including mappings assigned by ECS.  Bingo!

## Ten Easy Steps

### One
Create a ECS-friendly IAM role.  There are more restrictive and secure permissions that will likely work, but IAMFullAccess  + AmazonEC2ContainerServiceFullAccess will be idiot-proof.

### Two (optional)
Create and configure a VCP for your Docker "ocean".  The details of how to create a VPC are left to the reader but there's no specific magic here.  The default VPC will work fine for experimentation but in a real deployment you'll want your own VPC.

### Three
Create a specific security group for your Docker AMIs having open ports within the *assignment range*.  For added security you are advised to constraing inbound traffic to VPC-internal only, but that's up to you.

The assignment range is the range of ports ECS will auto-assign for you.  To find out what these are you actually have to see how this is set up on your EC2 image, which we haven't come to yet, so there's a little chicken 'n egg thing here.  This information is found in the file /proc/sys/net/ipv4/ip_local_port_range.  The defaults are likely fine, but the range can be set manually with the command 

`sudo sysctl -w net.ipv4.ip_local_port_range="49153 65535"`

On my sample instances the default rage was 32768 - 61000.   Try this value in the security group (open ports in this range).
         
### Four
Create a base AMI by starting with the EC2 ECS AMI (Amazon ECS-Optimized Amazon Linux AMI in the Amazon marketplace).  Launch the instance being sure to assign the IAM and security group you created!

Once it starts, log in and set up Docker introspection on port 5555.  **WARNING: This is insecure on an open network so be sure port 5555 is only accessible on this host!**  Edit the file /etc/sysconfig/docker adding the line:

```OPTIONS="-H 0.0.0.0:5555 -H unix:///var/run/docker.sock"```

Restart and verify this works with:

```curl http://<host_ip>:5555/containers/json```

You should see 1 block of information (for the running Amazon ECS agent).  Save a snapshot of this EC2 instance as your base AMI for Akka and Docker.

### Five
Run a couple more of these AMIs, again being sure to launch them with the IAM role and security group you set up.  This will be the initial pool of server resources available to your ocean.  If this is a "real" environment you have hopefully already set up a VPC so be sure to launch your instances configured to run in the desired VPC.

### Six
Create a new cluster if you're not interested in the default VPC/cluster.  Go to the ECS dashboard and confirm that your launched instances are running in the appropriate cluster.  If you launched instance into the default VPC you should see your instance already in the default cluster.  If not, you probably muffed the previous steps.

### Seven
Create a new task with your Docker container's URI (quay.io/gzoller/root:latest) and for this example make sure ports 8080 and 2551 are mapped to host port 0 (auto-assigned).  Also add environment variable EXT_AKKA with value true so you can expose Akka external to ECS for testing.  In a real deployment you would not set  EXT_AKKA as you're likely only accessing Akka within the AWS universe.

### Eight
Start the task and wait.  Once started log onto an instance and do `sudo docker inspect <inst_id>` to see the port mappings that ECS assigned.

### Nine
From your local machine:

    curl http://<public_ip>:<assigned_http_port>/ping

You should see a sane 'pong' message.

### Ten
Test Akka with sbt test and fill in the instance's public IP and the assigned Akka and http ports you found with the docker inspect command in step Eight.  The test should work.

