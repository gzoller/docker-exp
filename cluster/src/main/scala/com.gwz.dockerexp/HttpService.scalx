package com.gwz.dockerexp

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.actor._
import akka.http.Http
import akka.http.model._
import akka.http.model.HttpMethods._
import akka.stream.scaladsl.Flow
import akka.stream.FlowMaterializer
import scala.concurrent.{Await, Future}
import akka.util.Timeout

case class HttpService(svr:DocSvr, iface:String, port:Int) {

	implicit val system = svr.system
	implicit val materializer = FlowMaterializer()
	implicit val t:Timeout = 15.seconds

	println("HTTP Service on port "+port)

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => HttpResponse(entity = s"""{"resp":"${svr.name} says pong"}""")
		case HttpRequest(GET, Uri.Path("/nodes"), _, _, _) => HttpResponse(entity = s"""{"nodes":[${svr.getNodes}]}""")
		case HttpRequest(GET, Uri.Path("/wire"), _, _, _)  => HttpResponse(entity = {
			val actor = svr.findRoleActor("n2").get 
			val s:String = Await.result( (actor ? "hey").asInstanceOf[Future[String]], 15.seconds)
			s
			})
		case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
	}

	val serverBinding = Http(system).bind(interface = iface, port = port)
	serverBinding.connections foreach { connection => connection handleWith { Flow[HttpRequest] map requestHandler } }
}
