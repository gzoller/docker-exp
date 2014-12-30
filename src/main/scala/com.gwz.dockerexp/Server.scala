package com.gwz.dockerexp

import akka.http.Http
import akka.http.model._
import akka.http.model.HttpMethods._
import akka.stream.scaladsl.Flow
import akka.stream.FlowMaterializer
import akka.util.Timeout
import scala.concurrent.duration._

import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }
import scala.sys.process._

trait DocSvr extends App {
	lazy val port = 9090
	val myHostname = java.net.InetAddress.getLocalHost().getHostAddress()
	val myHttpUri = "http://"+myHostname+":"+port+"/"
	implicit val system = ActorSystem( "dockerexp", 
		ConfigFactory.load().withFallback(ConfigFactory.parseString(
			s"""akka.remote.netty.tcp.hostname=$myHostname
			    akka.remote.netty.tcp.port=$port""") ))
	HttpService(this, myHostname, port)
}

case class HttpService(svr:DocSvr, iface:String, port:Int) {

	implicit val system = svr.system
	implicit val materializer = FlowMaterializer()
	implicit val t:Timeout = 15.seconds

	println("HTTP Service on port "+port)

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => HttpResponse(entity = s"""{"resp":"${svr.myHostname} says pong"}""")
		case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
	}

	val serverBinding = Http(system).bind(interface = iface, port = port)
	serverBinding.connections foreach { connection => connection handleWith { Flow[HttpRequest] map requestHandler } }
}

object Go extends App with DocSvr {
}