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
		resolvers					++= Dependencies.resolutionRepos,
		scalacOptions				:= Seq("-feature", "-deprecation", "-encoding", "UTF8", "-unchecked"),
		testOptions in Test += Tests.Argument("-oDF"),
		version 					:= "latest"
	)

	lazy val dockerStuff = Seq(
		maintainer := "Greg Zoller <fake@nowhere.com>",
		dockerBaseImage := "errordeveloper/oracle-jre",
		dockerEntrypoint := Seq("bin/go.sh"),
		dockerRepository := Some("quay.io/gzoller"),  // Must log into quay.io from docker command-line before doing docker:publish!
		dockerExposedPorts := Seq(2551,8080)
		)

	lazy val root = Project(id = "dockerexp",
		base = file(".")) aggregate(web)

/*
	lazy val cluster = project.in(file("cluster"))
		.enablePlugins(JavaAppPackaging)
		.settings(dockerStuff:_*)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				typesafe_config, scalajack, akka_http, akka_streams, akka_actor, akka_cluster, akka_remote, akka_slf4j, logback) ++ 
			dep_test(scalatest)
		)
*/

	lazy val web = project.in(file("web"))
		.enablePlugins(JavaAppPackaging)
		.settings(Seq(mappings in Universal += file("go.sh") -> "bin/go.sh"):_*)
		.settings(dockerStuff:_*)
		.settings(Seq( // clean out extra CMD [] in Dockerfile
			dockerCommands := dockerCommands.value.filterNot{
				case ExecCmd("CMD",args @ _*) => true
				case cmd => false
			}
			):_*)
		.settings(Seq(dockerCommands += ExecCmd("CMD","bin/web")):_*)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				typesafe_config, scalajack, akka_http, akka_streams, akka_actor, akka_cluster, akka_remote, akka_tools, akka_slf4j, logback) ++ 
			dep_test(scalatest)
		)
}