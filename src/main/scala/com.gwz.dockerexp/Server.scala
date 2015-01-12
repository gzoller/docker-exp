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

trait DocSvr {
	implicit var system:ActorSystem = null
	def appArgs:Array[String] = Array.empty[String]
	var name = ""
	val myHostname = java.net.InetAddress.getLocalHost().getHostAddress()
	var myHttpUri = ""

	def init() {
		NodeConfig parseArgs appArgs map{ nc =>
			val c = nc.config
			val httpPort = c.getInt("http.port")
			myHttpUri = "http://"+myHostname+":"+httpPort+"/"
			system = ActorSystem( "dockerexp", 
				ConfigFactory.load().withFallback(ConfigFactory.parseString(
					s"""akka.remote.netty.tcp.hostname=$myHostname
					    akka.remote.netty.tcp.port=2551""") ))
			HttpService(this, myHostname, httpPort)
		}
	}
}

case class HttpService(svr:DocSvr, iface:String, port:Int) {

	implicit val system = svr.system
	implicit val materializer = FlowMaterializer()
	implicit val t:Timeout = 15.seconds

	println("HTTP Service on port "+port)

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => HttpResponse(entity = s"""{"resp":"${svr.name} says pong"}""")
		case HttpRequest(GET, Uri.Path("/stats"), _, _, _) => HttpResponse(entity = getStats())
		case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
	}

	val serverBinding = Http(system).bind(interface = iface, port = port)
	serverBinding.connections foreach { connection => connection handleWith { Flow[HttpRequest] map requestHandler } }

	private def getStats() = {
		"Cores       : "+Runtime.getRuntime().availableProcessors()+"\n"+
		"Free Memory : "+Runtime.getRuntime().freeMemory()+"\n"+
		"Max Memory  : "+Runtime.getRuntime().maxMemory()+"\n"+
		"Total Memory: "+Runtime.getRuntime().totalMemory()+"\n"
	}
}

object Go extends App with DocSvr {
	override def appArgs = args
	init()
}