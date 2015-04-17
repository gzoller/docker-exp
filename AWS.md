# Working with AWS

### Setup
There are of course many different configurations and setups possible.  This document describes the setup I used for these tests.  

First thing you need is a public Docker repository.  I opted to use quay.io.  Accounts for public repos are free.  In Build.scala I configured this with
 
```dockerRepository := Some("quay.io/gzoller")```

Oh... one important detail that's easy to overlook:  Be sure your instances in AWS have security policies that allow for access on ports 9100 and 9101 (for this Docker demo) for Akka and HTTP respectively.

### Building
Before running sbt, from your terminal you need to login to the quay.io repository:

```docker login quay.io```

Provide the required credentials for your account.

Now you can fire up sbt and do 

```docker:publish```

### Running
Log into your AWS instance (which should already have Docker installed) and follow these steps:

```
docker login quay.io
docker run -d -e "HOST_IP=`wget -qO- http://instance-data/latest/meta-data/public-ipv4`" -p 9100:2551 -p 9101:8080 quay.io/gzoller/root --name Fred --hostPort 9100
```

Where quay.io/gzoller is the name of your Docker hosted in Quay.
