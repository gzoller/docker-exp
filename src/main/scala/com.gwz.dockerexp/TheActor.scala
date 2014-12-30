package com.gwz.dockerexp

import akka.actor._

class TheActor(svr:DocSvr) extends Actor {

	def receive = {

		case "hey" => sender ! svr.name+" says 'you'"

	}
}