package com.gwz.dockerexp

import akka.io.IO
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout

import spray.can.Http
import spray.http._
import spray.httpx.RequestBuilding._
import HttpMethods._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object Util {

	def httpGet( uri:String )(implicit s:ActorSystem, timeout:Timeout = 30 seconds) = {
		val resp = _http( Get(uri) )
		(resp.entity.asString, resp.status)
	}
	def httpPost  ( url:String, payload:String, headers:Option[List[HttpHeader]] = None )(implicit system:ActorSystem) : HttpResponse = _http(HttpRequest(POST,   Uri(url), headers.getOrElse(Nil), HttpEntity(payload)))
	def httpPut   ( url:String, payload:String, headers:Option[List[HttpHeader]] = None )(implicit system:ActorSystem) : HttpResponse = _http(HttpRequest(PUT,    Uri(url), headers.getOrElse(Nil), HttpEntity(payload)))
	def httpDelete( url:String,                 headers:Option[List[HttpHeader]] = None )(implicit system:ActorSystem) : HttpResponse = _http(HttpRequest(DELETE, Uri(url), headers.getOrElse(Nil)))

	private def _http( hr:HttpRequest )(implicit s:ActorSystem, timeout:Timeout = 30 seconds) = {
		val response: Future[HttpResponse] = (IO(Http) ? hr).mapTo[HttpResponse]
		val allDone = Await.result( response, Duration.Inf )
		allDone
		}
}