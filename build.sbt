enablePlugins(JavaAppPackaging, JavaAgent)

val http4s = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.18.9",
  "org.http4s" %% "http4s-blaze-client" % "0.18.9",
  "org.http4s" %% "http4s-dsl"          % "0.18.9",
  "org.http4s" %% "http4s-circe"        % "0.18.9"
)

val bucky = Seq(
  "com.itv" %% "bucky-rabbitmq" % "1.3.1",
  "com.itv" %% "bucky-circe"    % "1.3.1",
  "com.itv" %% "bucky-fs2"      % "1.3.1",
  "com.itv" %% "bucky-test"     % "1.3.1" % "test, it"
)

val fs2 = Seq(
  "co.fs2" %% "fs2-core" % "0.10.4"
)

val test = Seq(
  "org.scalactic"  %% "scalactic" % "3.0.5",
  "org.scalatest"  %% "scalatest" % "3.0.5" % "test,it",
  "com.h2database" % "h2"         % "1.4.197" % "it",
  "org.scalamock"  %% "scalamock" % "4.1.0" % "test",
)

val circe = Seq(
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser"  % "0.9.3",
)

val doobie = Seq(
  "org.tpolecat" %% "doobie-core"      % "0.5.3",
  "org.tpolecat" %% "doobie-hikari"    % "0.5.3",
  "org.tpolecat" %% "doobie-postgres"  % "0.5.3",
  "org.tpolecat" %% "doobie-scalatest" % "0.5.3"
)

val logging = Seq(
  "org.log4s"      %% "log4s"          % "1.6.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback"       % "logstash-logback-encoder"   % "5.2"
)

val flyway = Seq(
  "org.flywaydb" % "flyway-core" % "5.1.4"
)

val config = Seq(
  "com.typesafe" % "config" % "1.3.2"
)

val kamon = Seq(
  "io.kamon"       %% "kamon-executors"      % "1.0.0",
  "io.kamon"       %% "kamon-logback"        % "1.0.2",
  "io.kamon"       %% "kamon-core"           % "1.1.2",
  "io.kamon"       %% "kamon-http4s"         % "1.0.8",
  "io.kamon"       %% "kamon-jaeger"         % "1.0.2",
  "io.kamon"       %% "kamon-zipkin"         % "1.0.0",
  "io.kamon"       %% "kamon-influxdb"       % "1.0.2",
  "io.kamon"       %% "kamon-jdbc"           % "1.0.2",
  "io.kamon"       %% "kamon-influxdb"       % "1.0.2",
  "io.kamon"       %% "kamon-system-metrics" % "1.0.0",
)

val commonConfig = Seq(
  scalaVersion := "2.12.6",
  organization := "com.example"
)

lazy val common = (project in file("./common"))
  .configs(IntegrationTest)
  .settings(
    commonConfig,
    name := "common",
    Defaults.itSettings,
    libraryDependencies ++= http4s ++ bucky ++ test ++ doobie ++ logging ++ kamon
  )

lazy val bartender = (project in file("./bartender"))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(JavaAgent)
  .settings(
    commonConfig,
    name := "bartender",
    mainClass in (Compile, packageBin) := Some("com.example.bartender.Main"),
    Defaults.itSettings,
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    libraryDependencies ++= http4s ++ bucky ++ test ++ config ++ fs2 ++ logging
  )
  .dependsOn(common)

lazy val console = (project in file("./console"))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(JavaAgent)
  .settings(
    commonConfig,
    name := "console",
    mainClass in (Compile, packageBin) := Some("com.example.console.Main"),
    Defaults.itSettings,
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    libraryDependencies ++= http4s ++ bucky ++ test ++ doobie ++ flyway ++ config ++ fs2 ++ logging
  )
  .dependsOn(common)

lazy val tap = (project in file("./tap"))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(JavaAgent)
  .settings(
    commonConfig,
    name := "tap",
    mainClass in (Compile, packageBin) := Some("com.example.tap.Main"),
    Defaults.itSettings,
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    libraryDependencies ++= http4s ++ bucky ++ test ++ config ++ fs2 ++ logging
  )
  .dependsOn(common)
