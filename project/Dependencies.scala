import sbt._

object Dependencies {
	import Versions._

	val resolutionRepos = Seq(
		"Typesafe Repo" 	at "http://repo.typesafe.com/typesafe/releases/",
		"OSS"				at "http://oss.sonatype.org/content/repositories/releases",
		"Spray"				at "http://repo.spray.io",
		"Mvn" 				at "http://mvnrepository.com/artifact"  // for commons_exec
	)

	def dep_compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
	def dep_test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")  // ttest vs test so as not to confuse w/sbt 'test'

	val akka_actor		= "com.typesafe.akka"		%% "akka-actor"			% Akka
	val akka_slf4j 		= "com.typesafe.akka" 		%% "akka-slf4j"			% Akka
	val akka_remote		= "com.typesafe.akka" 		%% "akka-remote"		% Akka
	val spray_can 	 	= "io.spray"				%% "spray-can" 			% Spray
	val spray_client	= "io.spray"				%% "spray-client"		% Spray
	val spray_routing	= "io.spray"				%% "spray-routing"		% Spray
	val spray_caching	= "io.spray"				%% "spray-caching"		% Spray
	val typesafe_config	= "com.typesafe"			% "config"				% Config

	val logback	        = "ch.qos.logback" 			% "logback-classic"		% Logback

	val scalatest 		= "org.scalatest" 			%% "scalatest"			% ScalaTest
	val slf4j_simple 	= "org.slf4j" 				% "slf4j-simple" 		% Slf4j
}
