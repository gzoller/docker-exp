package com.gwz.dockerexp

import org.scalatest._
import org.scalatest.Matchers._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, ActorContext}
import java.net.InetAddress
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps

package object MyAddr {
	val myaddr = InetAddress.getLocalHost().getHostAddress()
}
import MyAddr._

case class SeedServer() extends DocSvr {
	override def appArgs = Array("--seed","--name","Fred","--hostIP",myaddr,"--hostPort","8100","--httpPort","8101")
	init()
}
case class N1Server() extends DocSvr {
	override def appArgs = Array("--name","Barney","--hostIP",myaddr,"--hostPort","8200","--httpPort","8201","--roles","node,n1",myaddr+":8100")
	init()
}
case class N2Server() extends DocSvr {
	override def appArgs = Array("--name","Wilma","--hostIP",myaddr,"--hostPort","8300","--httpPort","8301","--roles","node,n2",myaddr+":8100")
	init()
}

class DockTests extends FunSpec with BeforeAndAfterAll with GivenWhenThen {

	val seed = SeedServer()
	var n1:DocSvr = null
	var n2:DocSvr = null
	implicit val t:Timeout = 15.seconds

	val testConfig = ConfigFactory parseString """
		akka {
			loglevel = "ERROR"
			stdout-loglevel = "ERROR"
			loggers = ["akka.event.slf4j.Slf4jLogger"]
			actor {
				provider = akka.remote.RemoteActorRefProvider
			}
			remote {
				enabled-transports = ["akka.remote.netty.tcp"]
			}
		}"""
	implicit val ss = ActorSystem("test",testConfig)

	override def afterAll() {
		Thread.sleep(1000)
		stop(ss)
		stop(seed.system)
		stop(n1.system)
	}

	def stop( s:ActorSystem ) {
		val whenTerminated = s.terminate
		Await.result(whenTerminated, 5 seconds)
	}

	describe("========= Test It!") {
		it("should ping") {
			println("Checking: "+seed.myHttpUri+"ping")
			println( Util.httpGet( seed.myHttpUri+"ping" ) )
		}
		it("should akka") {
			val actor = ss.actorSelection( seed.akkaUri+"/user/dockerexp" )
			println( Await.result( (actor ? "hey").asInstanceOf[Future[String]], 7.seconds) )
		}
		it("should know its other nodes") {
			n1 = N1Server()
			n2 = N2Server()
			Thread.sleep(2000)
			Util.httpGet( n1.myHttpUri+"nodes" )._2.split(",").length should be(3)
		}
		it("send wire message") {
			Util.httpGet( n1.myHttpUri+"wire" )._2 should equal("Wilma says 'you'")
		}
		it("kill one") {
			stop(n2.system)
			Thread.sleep(7000)
			Util.httpGet( seed.myHttpUri+"nodes" )._2.split(",").length should be(2)
		}
	}
}
