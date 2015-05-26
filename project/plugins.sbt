resolvers += Resolver.url("bintray-sbt-plugin-releases", url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "1.1.29")