import sbt._
import Keys._    

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

object Build extends Build {
	import Dependencies._
	import Versions._

	lazy val basicSettings = Seq(
 		organization 				:= "com.gwz",
		description 				:= "Playing with Docker",
		startYear 					:= Some(2014),
		scalaVersion 				:= Scala,
		parallelExecution in Test 	:= false,
		fork in Test 				:= true,
		javaOptions in Test         := Seq("-Xmx3m"),
		resolvers					++= Dependencies.resolutionRepos,
		scalacOptions				:= Seq("-feature", "-deprecation", "-encoding", "UTF8", "-unchecked"),
		testOptions in Test += Tests.Argument("-oDF"),
		version 					:= "latest"
	)

	// errordeveloper/oracle-jdk
	// errordeveloper/oracle-jre
	lazy val dockerStuff = Seq(
		maintainer := "Greg Zoller <fake@nowhere.com>",
		dockerBaseImage := "errordeveloper/oracle-jre",
		dockerExposedPorts in Docker := Seq(9090),
		dockerRepository := Some("localhost:5000")
		// dockerEntrypoint in Docker := Seq("java")
		)

	lazy val root = project.in(file("."))
		.enablePlugins(JavaAppPackaging)
		.settings(dockerStuff:_*)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				typesafe_config, twitter, logback_core, logback, scopt, akka_http, akka_streams, akka_actor, akka_remote, akka_slf4j, akka_cluster) ++ 
			dep_test(scalatest)
		)
}