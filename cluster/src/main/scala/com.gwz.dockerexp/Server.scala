package com.gwz.dockerexp

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl.{Flow,Sink,Source}
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await,Future}
import scala.collection.JavaConversions
import scala.util.Try
import scala.collection.JavaConversions._

import akka.actor._
import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import scala.sys.process._

trait DocSvr {
	val clusterSystemName = "dockerexp"
	val ssn = java.util.UUID.randomUUID.toString
	val ipAndPort = IpAndPort()
	implicit val system:ActorSystem

	def init( seeds:List[String] ) : ActorSystem = {
		val c = ConfigFactory.load()
			.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(java.net.InetAddress.getLocalHost().getHostAddress()))
			.withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(ipAndPort.hostIP))
			.withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(ipAndPort.akkaPort))
			.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(
				JavaConversions.asJavaIterable(seeds.map(seedLoc => s"akka.tcp://$clusterSystemName@$seedLoc").toIterable)
				))
		println(s"------ $ssn ------")
		// println("Starting core on "+c.getString("akka.remote.netty.tcp.hostname")+" port "+c.getString("akka.remote.netty.tcp.port"))
		println("Binding core internally on "
			+c.getString("akka.remote.netty.tcp.bind-hostname")
			+" port "
			+c.getString("akka.remote.netty.tcp.bind-port"))
		println("Binding core externally on "
			+c.getString("akka.remote.netty.tcp.hostname")
			+" port "
			+c.getString("akka.remote.netty.tcp.port"))
		// println("Local (inside) addr: "+java.net.InetAddress.getLocalHost().getHostAddress())
		println("Seeds: "+c.getList("akka.cluster.seed-nodes").toList)
		println("Roles: "+c.getList("akka.cluster.roles").toList)
		val as = ActorSystem( clusterSystemName, c )
		println("My Akka URI: " + as.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress)  // warning: unorthodox mechanism
		as
		// if( !isSeed ) {
		// 	val	myActor = as.actorOf(Props(new LogicActor(this)), "logic")
		// 	ClusterReceptionistExtension(as).registerService(myActor)
		// 	val akkaUri = as.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
		// 	println("Akka URI: "+akkaUri)
		// }
		// as
	}

	var nodes = Set.empty[akka.cluster.Member]  // cluster node registry
	def getNodes() = nodes.map( m => m.address+" "+m.getRoles.mkString ).mkString(",")
}

case class SeedNode() extends DocSvr {
	implicit val system = init(List(ipAndPort.hostIP+":"+ipAndPort.akkaPort))
}
case class LogicNode(seedLoc : String) extends DocSvr {
	implicit val system = init(List(seedLoc))
	system.actorOf(Props(new LogicActor(this)), "logic")
}
case class RestNode(seedLoc : String) extends DocSvr {
	import akka.cluster.pubsub._

	implicit val system = init(List(seedLoc))
	implicit val materializer = ActorMaterializer()
	implicit val t:Timeout = 15.seconds

	system.actorOf(Props(new ClusterActor(this)), "cluster")

	val port  = ConfigFactory.load().getInt("settings.http")
	val iface = java.net.InetAddress.getLocalHost().getHostAddress()
	val mediator = DistributedPubSub(system).mediator

	val requestHandler: HttpRequest ⇒ HttpResponse = {
		case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  => 
			HttpResponse(entity = s"""{"resp":"${ssn} says pong"}""")
		case HttpRequest(GET, Uri.Path("/nodes"), _, _, _)  => 
			HttpResponse(entity = s"""{"nodes":"[${getNodes()}]}""")
		case HttpRequest(GET, Uri.Path("/svc"), _, _, _)  => {
			println("Making logic call with AKKA...")
			val resp = Await.result(mediator ? DistributedPubSubMediator.Send("/user/logic","hey",false), t.duration).asInstanceOf[String]
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

// You can start this server either as a seed node or a "logic" node.  In this trivial example only one seed
// is supported but for a real application you'd want a more sophisticaed capability to pass in >1 seed node
// for your logic nodes.
object Go extends App {
	args(0) match {
		case "seed"   => SeedNode()
		case "rest"   => RestNode(args(1))
		case "logic"  => LogicNode(args(1))
	}
}
