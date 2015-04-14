package com.boom

import akka.actor._
import scala.concurrent.{Await,Future}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }

object Talker extends App {
    val c = ConfigFactory parseString """akka {
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
    
  val sys = ActorSystem( "boom", c )
  val akkaUri = sys.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
  println("Akka: "+akkaUri)

  val IP = "172.16.240.141"

  val actor = sys.actorSelection(s"akka.tcp://dockerexp@$IP:9100/user/dockerexp")
  println("Actor: "+actor)
  implicit val timo = Timeout(5.seconds)
  try { 
    println( Await.result( (actor ? "hey").asInstanceOf[Future[String]], 15.seconds) )
  } finally {
    println("Dying...")
    Thread.sleep(5000)
    sys.shutdown()
  }
}