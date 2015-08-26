package com.gwz.dockerexp

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

import akka.cluster.{ Member, MemberStatus, Cluster }
import akka.cluster.ClusterEvent._

class ClusterActor(svr:DocSvr) extends Actor {
	val cluster = Cluster(context.system)

	// subscribe to cluster events
	override def preStart():Unit = {
		println("== Starting cluster listener 2 ==")
		cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
			// classOf[MemberUp], classOf[MemberRemoved], classOf[UnreachableMember], classOf[MemberExited])
	}

	override def postStop():Unit = cluster.unsubscribe(self)

	def receive = {

		// cluster event sent when a new cluster member comes up. Register the new cluster member if it is the parent node
		case state   : MemberUp       => {
			println("--1-- "+state.member)
			svr.nodes.synchronized{ svr.nodes += state.member }
		}

		// cluster event sent when a cluster member is removed. Unregister the cluster member if it is the parent node
		case state   : MemberRemoved  => {
			println("--2--")
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}

		case state   : UnreachableMember  => {
			println("--3--")
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}

		case state   : MemberExited  => {
			println("--4--")
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}

		case z => println(s"Hmm... unknown event $z")
	}	
}

class LogicActor(svr:DocSvr) extends Actor {

	val mediator = DistributedPubSub(context.system).mediator
	// subscribe to the topic named "logicTopic"
	mediator ! DistributedPubSubMediator.Put(self)

	def receive = {
		case "hey" => 
			println("Received hey message...")
			sender ! svr.ssn+" says 'you'"
	}
/*
	val cluster = Cluster(context.system)

	// subscribe to cluster events
	override def preStart():Unit = 
		cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
			classOf[MemberUp], classOf[MemberRemoved], classOf[UnreachableMember], classOf[MemberExited])

	override def postStop():Unit = cluster.unsubscribe(self)

	def receive = {

		case "hey" => sender ! svr.name+" says 'you'"

		// cluster event sent when a new cluster member comes up. Register the new cluster member if it is the parent node
		case state   : MemberUp       => {
			println("Member Up: "+svr.name+" "+state.member)
			// println("UP: "+state.member.address)
			svr.nodes.synchronized{ svr.nodes += state.member }
		}

		// cluster event sent when a cluster member is removed. Unregister the cluster member if it is the parent node
		case state   : MemberRemoved  => {
			// println("!!! Member Removed: "+svr.name+" "+state.member)
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}

		case state   : UnreachableMember  => {
			// println("!!! Unreachable: "+svr.name+" "+state.member)
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}

		case state   : MemberExited  => {
			// println("!!! Member Exited: "+svr.name+" "+state.member)
			svr.nodes.synchronized{ svr.nodes -= state.member }
		}
	}
	*/
}