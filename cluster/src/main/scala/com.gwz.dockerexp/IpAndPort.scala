package com.gwz.dockerexp

import java.net._
import java.io._
import co.blocke.scalajack._
import scala.util.Try

package object IpPort {
	val AWS_LOCAL_HOST_IP  = "http://169.254.169.254/latest/meta-data/local-ipv4"
	val AWS_PUBLIC_HOST_IP = "http://169.254.169.254/latest/meta-data/public-ipv4"
	val INTERNAL_AKKA_PORT = 2551
	val PORTSTER_SVC       = 1411

	val ENV_HOST_IP        = "HOST_IP"
	val ENV_HOSTNAME       = "HOSTNAME"
	val ENV_EXT_AKKA       = "EXT_AKKA"
	val ENV_SYSTEM_NAME    = "SYSTEM_NAME"

	def httpGetLite( uri:String ) = Try{scala.io.Source.fromURL(uri,"utf-8").getLines.fold("")( (a,b) => a + b )}.toOption
}
import IpPort._

case class IpAndPort() {

	// Use Portster service to get Akka port
	private val myIP = java.net.InetAddress.getLocalHost().getHostAddress()
	val akkaPort = IpPort.httpGetLite( s"http://$myIP:${IpPort.PORTSTER_SVC}/port/${IpPort.INTERNAL_AKKA_PORT}" )
		.map( _.toInt )
		.getOrElse( throw new FailException("Can't determine Akka port") )

	val hostIP = Option(System.getenv(IpPort.ENV_HOST_IP)).orElse{
		// Try AWS
		if( System.getenv().get(ENV_EXT_AKKA) == "true" )
			IpPort.httpGetLite(AWS_PUBLIC_HOST_IP)  // Akka callable only outside AWS
		else
			IpPort.httpGetLite(AWS_LOCAL_HOST_IP)   // Akka callable only inside AWS
	}.getOrElse(throw new FailException("Can't determine host IP"))
}

class FailException(msg:String) extends Exception(msg)