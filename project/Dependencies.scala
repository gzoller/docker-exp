import sbt._

object Dependencies {
	import Versions._

	val resolutionRepos = Seq(
		"Typesafe Repo" 	at "http://repo.typesafe.com/typesafe/releases/",
		"Akka Snapshots"	at "http://repo.akka.io/snapshots/",
		"OSS"				at "http://oss.sonatype.org/content/repositories/releases",
		"Mvn" 				at "http://mvnrepository.com/artifact"  // for commons_exec
	)

	def dep_compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
	def dep_test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")  // ttest vs test so as not to confuse w/sbt 'test'

	val akka_actor		= "com.typesafe.akka"		%% "akka-actor"		% Akka
	val akka_slf4j 		= "com.typesafe.akka" 		%% "akka-slf4j"		% Akka
	val akka_remote		= "com.typesafe.akka" 		%% "akka-remote"	% Akka
	val akka_cluster	= "com.typesafe.akka" 		%% "akka-cluster" 	% Akka
	val akka_streams	= "com.typesafe.akka" 		%% "akka-stream-experimental" % "1.0-M5"
	val akka_http		= "com.typesafe.akka" 		%% "akka-http-core-experimental" % "1.0-M5"	
	val typesafe_config	= "com.typesafe"			% "config"				% Config
	val scopt			= "com.github.scopt" 		%% "scopt" 				% Scopt

	val logback	        = "ch.qos.logback" 			% "logback-classic"		% Logback

	val scalatest 		= "org.scalatest" 			%% "scalatest"			% ScalaTest
	val slf4j_simple 	= "org.slf4j" 				% "slf4j-simple" 		% Slf4j
}
