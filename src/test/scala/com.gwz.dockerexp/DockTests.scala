package com.gwz.dockerexp

import org.scalatest._
import org.scalatest.Matchers._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.sys.process._
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, ActorContext}
import akka.pattern.ask
import akka.util.Timeout

case class TestServer() extends DocSvr 

class DockTests extends FunSpec with BeforeAndAfterAll with GivenWhenThen {

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
					port     = 5151
				}
			}
		}"""
	implicit val ss = ActorSystem("test",testConfig)
	val IP = java.net.InetAddress.getLocalHost().getHostAddress()

	val awsIP = scala.io.StdIn.readLine("Enter IP of Docker running on AWS (nothing for none): ")
	val awsAkkaPort = {
		if( awsIP != "" )
			scala.io.StdIn.readLine("Enter Akka port for Docker running on AWS (nothing for none): ")
		else ""
	}
	val awsHttpPort = {
		if( awsIP != "" )
			scala.io.StdIn.readLine("Enter HTTP port for Docker running on AWS (nothing for none): ")
		else ""
	}

	override def beforeAll() {
		// Run Docker
		s"""docker run -d -e HOST_IP=$IP -e HOST_PORT=9101 -e INST_NAME=Fred --name=dockerexp -p 9101:2551 -p 8101:8080 quay.io/gzoller/root""".!
		Thread.sleep(3000)
	}

	override def afterAll() {
		// Shut down actor and Docker
		Thread.sleep(1000)
		stop(ss)
		println("Stopping & removing docker")
		"docker stop dockerexp".!
		"docker rm dockerexp".!
		println("Done")
	}

	def stop( s:ActorSystem ) {
		val whenTerminated = s.terminate
		Await.result(whenTerminated, 5 seconds)
	}

	describe("========= Docker Tests") {
		describe("::: Running Locally (do docker:publishLocal before running tests)") {
			it("should ping") {
				Util.httpGetLite( s"http://$IP:8101/ping" ) should equal( Some("""{"resp":"Fred says pong"}""") )
			}
			it("should akka") {
				val actor = ss.actorSelection( s"akka.tcp://dockerexp@$IP:9101/user/dockerexp" )
				Await.result( (actor ? "hey").asInstanceOf[Future[String]], 5.seconds) should equal("Fred says 'you'")
			}
		}
		describe("::: Running in AWS (do docker:publish before running tests)") {
			it("should ping") {
				if( awsIP != "" )
					Util.httpGetLite( s"http://$awsIP:$awsHttpPort/ping" ).get.endsWith("""says pong"}""") should be( true )
			}
			it("should akka") {
				if( awsIP != "" ) {
					val actor = ss.actorSelection( s"akka.tcp://dockerexp@$awsIP:$awsAkkaPort/user/dockerexp" )
					val z = Await.result( (actor ? "hey").asInstanceOf[Future[String]], 5.seconds)
					println("Z: "+z)
					z.endsWith(""" says 'you'""") should be( true )
					// Await.result( (actor ? "hey").asInstanceOf[Future[String]], 5.seconds).endsWith(""" says 'you'""") should be( true )
				}
			}
		}
	}
}
