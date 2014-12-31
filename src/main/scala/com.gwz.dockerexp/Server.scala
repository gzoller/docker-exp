package com.gwz.dockerexp

import akka.http.Http
import akka.http.model._
import akka.http.model.HttpMethods._
import akka.stream.scaladsl.Flow
import akka.stream.FlowMaterializer
import akka.util.Timeout
import akka.cluster.Member
import scala.collection.JavaConversions._
import scala.concurrent.duration._

import akka.actor._
import com.typesafe.config.{ Config, ConfigFactory }
import scala.sys.process._

trait DocSvr {
	implicit var system:ActorSystem = null
	def appArgs:Array[String] = Array.empty[String]
	var name = ""
	val myHostname = java.net.InetAddress.getLocalHost().getHostAddress()
	var myHttpUri = ""
	var akkaUri:Address = null

	def init() {
		NodeConfig parseArgs appArgs map{ nc =>
			val c = nc.config
			//println(c)
			name = c.getString("dkr.name")
			val httpPort = c.getInt("http.port")
			val akkaPort = c.getInt("dkr.port")

			println(s"------ $name ------")
			// println("Starting core on "+c.getString("akka.remote.netty.tcp.hostname")+" port "+c.getString("akka.remote.netty.tcp.port"))
			println("Binding core on "
				+c.getString("akka.remote.netty.tcp.bind-hostname")
				+" port "
				+c.getString("akka.remote.netty.tcp.bind-port"))
			println("Local (inside) addr: "+java.net.InetAddress.getLocalHost().getHostAddress())
			println("Seeds: "+c.getList("akka.cluster.seed-nodes").toList)
			println("Roles: "+c.getList("akka.cluster.roles").toList)

			myHttpUri = "http://"+myHostname+":"+httpPort+"/"
			system = ActorSystem( "dockerexp", c)

			akkaUri = system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
			println("AKKA: "+akkaUri)

			system.actorOf(Props(new TheActor(this)), "dockerexp")

			HttpService(this, myHostname, httpPort)
		}
	}
	//-------------------
	var nodes = Set.empty[Member]  // cluster node registry
	def getNodes() = nodes.map( m => m.address+" "+m.getRoles.mkString ).mkString(",")
	def findRoleActor( r:String ) = 
		nodes.find( _.hasRole(r) ).map( m => system.actorSelection(RootActorPath(m.address) / "user" / "dockerexp") )
}

object Go extends App with DocSvr {
	override def appArgs = args
	init()
}