package com.gwz.dockerexp

import com.typesafe.config._
import scopt._

case class NodeConfig(
	name       : String      = "noname",
	hostIP     : String      = "127.0.0.1",
	hostPort   : Int         = 2551,
	httpPort   : Int         = 8080,	// default fine inside Docker
	isSeed     : Boolean     = false,
	roles      : Seq[String] = Seq.empty[String],
	seeds      : Seq[String] = Seq.empty[String]
) {
	import ConfigFactory._
	import NodeConfig._

	// Initialize the config once
	lazy val config = asConfig

	private def asConfig(): Config = {
		val config = load( getClass.getClassLoader, ConfigResolveOptions.defaults.setAllowUnresolved(true) )

		val serverNameValue = ConfigValueFactory fromAnyRef name

		// HTTP port
		val httpPortValue = ConfigValueFactory fromAnyRef httpPort

		// Host IP/port
		val hostIPValue = ConfigValueFactory fromAnyRef hostIP
		val hostPortValue = ConfigValueFactory fromAnyRef hostPort
		val localIP = java.net.InetAddress.getLocalHost().getHostAddress()
		val localIPValue = ConfigValueFactory fromAnyRef localIP

		// add seed nodes to config
		val seedStart = {
			if( isSeed ) Seq(s"$hostIP:$hostPort")
			else Seq.empty[String]
		}
		val seedNodesString = seedStart.union(seeds).distinct.map { seedNode =>
			s"""akka.cluster.seed-nodes += "akka.tcp://dockerexp@$seedNode""""  //"
		}.mkString("\n")
		val nodeRoleString = roles.map { role =>
			s"""akka.cluster.roles += $role""" 
		}.mkString("\n")

		// build the final config and resolve it
		(ConfigFactory parseString seedNodesString)
			.withValue("dkr.name",     serverNameValue)
			.withValue("dkr.local",    localIPValue)
			.withValue("dkr.hostname", hostIPValue)
			.withValue("dkr.port",     hostPortValue)
			.withValue("http.port",    httpPortValue)
			.withFallback(ConfigFactory parseString nodeRoleString)
			.withFallback(config)
			.resolve
	}
}

object NodeConfig {
	val parser = new OptionParser[NodeConfig]("dockerexp") {
		head("dockerexp","1.x")
		opt[Unit]("seed") action { (_,c) =>
			c.copy( isSeed = true ) } text("set if this is a seed node")
		opt[String]("name") action { (x,c) =>
			c.copy( name = x ) } text("name of my stuff")
		opt[String]("hostIP") action { (x,c) =>
			c.copy( hostIP = x ) } text("IP address of container's host")
		opt[Int]("hostPort") action { (x,c) =>
			c.copy( hostPort = x ) } text("Akka port for container's host")
		opt[Int]("httpPort") action { (x,c) =>
			c.copy( httpPort = x ) } text("port for HTTP access")
		opt[Seq[String]]("roles") action { (x,c) =>
			c.copy( roles = x.toList ) } text("this node's roles")
		help("help") text("prints this usage text")
		arg[String]("<seedIP>...") unbounded() optional() action { (x,c) =>
			c.copy(seeds = c.seeds :+ x)} text("list of seed node IP addresses")	
		checkConfig { c =>
			if( c.seeds.length == 0 && !c.isSeed ) failure("Must be at least one seed node specified.")
			else success
		}
	}
	def parseArgs( args:Array[String] ) = {
		println("Parsing args: "+args.toList)
		parser.parse(args,NodeConfig())
	}
}
