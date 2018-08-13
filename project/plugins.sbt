addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.6")
resolvers += Resolver.bintrayRepo("kamon-io", "sbt-plugins")
addSbtPlugin("io.kamon"          % "sbt-aspectj-runner" % "1.1.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent"      % "0.1.4")
