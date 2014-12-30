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

case class DocSvr2() extends DocSvr {
	override def appArgs = Array("--httpPort","8101")
	init()
}

class DockTests extends FunSpec with BeforeAndAfterAll with GivenWhenThen {

	var server = DocSvr2()
	implicit val t:Timeout = 15.seconds
	println("SYS: "+server.system)
	implicit val system:ActorSystem = server.system

	override def beforeAll() {
		val x = server.system // force creation of lazy object
	}

	override def afterAll() {
		Thread.sleep(1000)
		server.system.shutdown
		server.system.awaitTermination
		assert(server.system.isTerminated)
	}

	describe("========= Test It!") {
		it("should work") {
			println("Checking: "+server.myHttpUri+"ping")
			println( Util.httpGet( server.myHttpUri+"ping" ) )
		}
	}
}
