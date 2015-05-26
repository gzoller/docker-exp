import sbt._
import Keys._    

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker._

object Build extends Build {
	import Dependencies._
	import Versions._

	val IP = java.net.InetAddress.getLocalHost().getHostAddress()

	lazy val basicSettings = Seq(
 		organization 				:= "com.gwz",
		description 				:= "Playing with Docker",
		startYear 					:= Some(2014),
		scalaVersion 				:= Scala,
		parallelExecution in Test 	:= false,
		fork in Test 				:= true,
		javaOptions in Test         := Seq("-DINST_NAME=Fred", s"-DCLUSTER_IP=$IP", "-DCLUSTER_PORT=8100", "-DHTTP_PORT=8101"),
		resolvers					++= Dependencies.resolutionRepos,
		scalacOptions				:= Seq("-feature", "-deprecation", "-encoding", "UTF8", "-unchecked"),
		testOptions in Test += Tests.Argument("-oDF"),
		version 					:= "latest"
	)

	lazy val dockerStuff = Seq(
		maintainer := "Greg Zoller <fake@nowhere.com>",
		dockerBaseImage := "errordeveloper/oracle-jre",
		dockerRepository := Some("quay.io/gzoller"),  // Must log into quay.io from docker command-line before doing docker:publish!
		dockerExposedPorts := Seq(2551,8080)
		)

	lazy val root = project.in(file("."))
		.enablePlugins(JavaAppPackaging)
		.settings(dockerStuff:_*)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				typesafe_config, scalajack, akka_http, akka_streams, akka_actor, akka_remote, akka_slf4j, logback) ++ 
			dep_test(scalatest)
		)
}