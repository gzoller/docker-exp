package com.gwz.dockerexp

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl.{Flow,Sink,Source}
import akka.stream.ActorFlowMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.util.Try

import akka.actor._
import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import scala.sys.process._

trait WebSvr {
	val ssn = java.util.UUID.randomUUID.toString
	val c = ConfigFactory.load()
	implicit val system = ActorSystem( "dockerexp", c )
	HttpService(this, java.net.InetAddress.getLocalHost().getHostAddress(), c.getInt("settings.http"))
}

case class HttpService(svr:WebSvr, iface:String, port:Int) {

	implicit val system = svr.system
	implicit val materializer = ActorFlowMaterializer()
	implicit val t:Timeout = 15.seconds

	println("HTTP Service on port "+port)

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => 
	println("Request!")
			HttpResponse(entity = s"""{"resp":"${svr.ssn} says pong"}""")
		case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
	}

	val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] = 
		Http(system).bind(interface = iface, port = port)
	val bindingFuture: Future[Http.ServerBinding] = serverSource.to(Sink.foreach { connection =>
		connection handleWithSyncHandler requestHandler
		// this is equivalent to
		// connection handleWith { Flow[HttpRequest] map requestHandler }
	}).run()
}

object Go extends App with WebSvr
