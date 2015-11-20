import sbt._
import Keys._    

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes._
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
		maintainer := "John Smith <fake@nowhere.com>",
		dockerBaseImage := "gzoller/akka-base",
		daemonUser in Docker  := "root",
		dockerRepository := Some("dockerexp") 
		)

	lazy val root = Project(id = "dockerexp",
		base = file(".")) aggregate(cluster)

	lazy val cluster = project.in(file("cluster"))
		.enablePlugins(AshScriptPlugin)
		.settings(isSnapshot := true)
		.settings(dockerStuff:_*)
		.settings(
			dockerExposedPorts := Seq(2551,8080,2552),
			dockerEntrypoint   := Seq("bin/cluster"),
			packageName in Docker := "cluster"
			)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				typesafe_config, scalajack, akka_http, akka_streams, akka_actor, akka_cluster, akka_tools, akka_contrib, akka_remote, akka_slf4j, logback) ++ 
			dep_test(scalatest)
		)
		.settings((Keys.test in Test) <<= (Keys.test in Test) dependsOn (publishLocal in Docker))
}