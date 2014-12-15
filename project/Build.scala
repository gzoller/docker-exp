import sbt._
import Keys._    

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
// import com.typesafe.sbt.packager.Keys._
// import DockerKeys._

object Build extends Build {
	import Dependencies._
	import Versions._

	// lazy val basicSettings = ScoverageSbtPlugin.instrumentSettings ++ Seq(
	lazy val basicSettings = Seq(
 		organization 				:= "com.gwz",
		description 				:= "Playing with Docker",
		startYear 					:= Some(2014),
		scalaVersion 				:= Scala,
		parallelExecution in Test 	:= false,
		fork in Test 				:= true,
		resolvers					++= Dependencies.resolutionRepos,
		scalacOptions				:= Seq("-feature", "-deprecation", "-encoding", "UTF8", "-unchecked"),
		testOptions in Test += Tests.Argument("-oDF"),
		version 					:= "1.0.0"
	)

	lazy val dockerStuff = Seq(
		maintainer := "Greg Zoller <fake@nowhere.com>",
		dockerBaseImage := "localhost:5000/java7-base",
		dockerExposedPorts in Docker := Seq(9090),
		dockerEntrypoint in Docker := Seq("sh", "-c", "CLUSTER_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` bin/clustering $*")
		dockerRepository := Some("localhost:5000")
		)

	lazy val root = project.in(file("."))
		.enablePlugins(JavaAppPackaging)
		.settings(dockerStuff:_*)
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			dep_compile(
				spray_routing, spray_client, spray_can, spray_caching,
				typesafe_config, akka_actor, akka_remote, akka_slf4j, logback) ++ 
			dep_test(scalatest, spray_client)
		)
}