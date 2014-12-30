package com.gwz.dockerexp

import org.scalatest._
import org.scalatest.Matchers._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, ActorContext}
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps

case class TestServer() extends DocSvr {
	override def appArgs = Array("--name","Fred","--hostPort","8100","--httpPort","8101")
	init()
}

class DockTests extends FunSpec with BeforeAndAfterAll with GivenWhenThen {

	val server = TestServer()
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

	override def beforeAll() {
		val x = server.system // force creation of lazy object
	}

	override def afterAll() {
		Thread.sleep(1000)
		stop(ss)
		stop(server.system)
	}

	def stop( s:ActorSystem ) {
		val whenTerminated = s.terminate
		Await.result(whenTerminated, 5 seconds)
	}

	describe("========= Test It!") {
		it("should ping") {
			println("Checking: "+server.myHttpUri+"ping")
			println( Util.httpGet( server.myHttpUri+"ping" ) )
		}
		it("should akka") {
			val actor = ss.actorSelection( server.akkaUri )
			println( Await.result( (actor ? "hey").asInstanceOf[Future[String]], 15.seconds) )
		}
	}
}
