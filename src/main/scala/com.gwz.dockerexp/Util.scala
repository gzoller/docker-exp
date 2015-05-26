package com.gwz.dockerexp

import akka.http.Http
import akka.http.model._

import akka.io.IO
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Sink, Source }

object Util {

	def httpGet( uri:String )(implicit s:ActorSystem) = {
		implicit val materializer = ActorFlowMaterializer()
		var r:HttpResponse = null
		val req = HttpRequest(HttpMethods.GET, Uri(uri))
		val host:String = req.uri.authority.host.toString
		val port:Int = req.uri.effectivePort
		val httpClient = Http().outgoingConnection(host,port)
		val consumer = Sink.foreach[HttpResponse] { resp ⇒ r = resp }
		val finishFuture = Source.single(req).via(httpClient).runWith(consumer)
		Await.result(finishFuture, Duration("3 seconds"))

		// unpack result
		(r.status.intValue,
			Await.result(r.entity.toStrict(FiniteDuration(3,"seconds")), Duration("3 seconds") )
				.data
				.utf8String)
	}
}