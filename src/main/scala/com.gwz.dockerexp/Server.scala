package com.gwz.dockerexp

import spray.routing._
import Directives._
import spray.http.MediaTypes._
import spray.http._
import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }

trait DocSvr extends SimpleRoutingApp {
	lazy val port = 9090
	val myHostname = java.net.InetAddress.getLocalHost().getHostAddress()
	implicit val system = ActorSystem( "dockerexp", 
		ConfigFactory.load().withFallback(ConfigFactory.parseString(
			s"""akka.remote.netty.tcp.hostname=$myHostname
			    akka.remote.netty.tcp.port=$port""") ))
	val myHttpUri  = "http://" + myHostname + ":" + port + "/"
	startServer(interface=myHostname, port=port, serviceActorName="docker" )( Service().route )
}

case class Service() {
	val route = 
		get {  
			respondWithMediaType(`application/json`) {
				path( "ping" ) { 
					complete{
						s"""{"resp":"pong : $name""}"""
					}
				}
				// ~path( "remoteping" ) { 
				// 	complete{
				// 		s"""{"resp":"pong : $name""}"""
				// 	}
				// }
				~path( "members" ) { 
					complete{
						"""{"resp":"pong"}"""
					}
				}
			}
		}
}

object Go extends App with DocSvr {
}