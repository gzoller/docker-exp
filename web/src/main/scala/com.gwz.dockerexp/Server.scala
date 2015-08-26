package com.gwz.dockerexp

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.cluster.client.{ClusterClient,ClusterClientSettings}
import akka.stream.scaladsl.{Flow,Sink,Source}
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await,Future}
import scala.collection.JavaConverters._
import scala.util.Try
import akka.actor._
import com.typesafe.config.ConfigFactory

// Rediculously trivial HTTP endpoint server that calls into the cluster (as a cluster client)
//
object Go extends App {

	val ssn = java.util.UUID.randomUUID.toString
	val c = ConfigFactory.load()
	val port = c.getInt("settings.http")
	val iface = java.net.InetAddress.getLocalHost().getHostAddress()

	implicit val system = ActorSystem( "restTier", c )
	implicit val materializer = ActorMaterializer()
	implicit val t:Timeout = 15.seconds

	// First passed-in argument is "host:port" of the cluster receptionist (the seed node of the cluster in this example, but doesn't have to be!)
	val initialContacts = Set(ActorPath.fromString(s"akka.tcp://dockerexp@${args.toList(0)}/user/receptionist"))
	val logicCluster = system.actorOf(ClusterClient.props(ClusterClientSettings(system).withInitialContacts(initialContacts)), "client")

	println("HTTP Service on port "+port)

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => 
			HttpResponse(entity = s"""{"resp":"${ssn} says pong"}""")
		case HttpRequest(GET, Uri.Path("/svc"), _, _, _)  => {
			val resp = Await.result(logicCluster ? ClusterClient.Send("/user/logic","id_msg",false), t.duration).asInstanceOf[String]
			HttpResponse(entity = s"""{"cluster_node":"$resp"}""")
		}
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
