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

class TalkerTests extends FunSpec with BeforeAndAfterAll with GivenWhenThen {

	implicit val t:Timeout = 15.seconds

	val testConfig = ConfigFactory parseString """
		akka {
			loglevel = "ERROR"
			stdout-loglevel = "ERROR"
			loggers = ["akka.event.slf4j.Slf4jLogger"]
			actor {
				provider = "akka.remote.RemoteActorRefProvider"
			}
			remote {
				enabled-transports = ["akka.remote.netty.tcp"]
				netty.tcp {
					hostname = "localhost"
					port     = 5152
				}
			}
		}"""
	implicit val ss = ActorSystem("test",testConfig)

	override def afterAll() {
		Thread.sleep(1000)
		stop(ss)
	}

	def stop( s:ActorSystem ) {
		val whenTerminated = s.terminate
		Await.result(whenTerminated, 5 seconds)
	}

	describe("========= Talker Tests") {
		it("should talk to remote node") {
			val HOST_IP = "52.11.63.42" // put AWS host IP here
			val actor = ss.actorSelection(s"akka.tcp://dockerexp@$HOST_IP:9101/user/dockerexp")
			println( Await.result( (actor ? "hey").asInstanceOf[Future[String]], 15.seconds) )
		}
	}
}
