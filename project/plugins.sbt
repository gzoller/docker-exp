//addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")

lazy val root = project.in(file(".")).dependsOn(packagerPlugin) 

lazy val packagerPlugin = uri("git://github.com/sbt/sbt-native-packager")
