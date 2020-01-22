name := "LolAssistant"
organization := "org.kys"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "jitpack" at "https://jitpack.io"

javaOptions in Compile ++= Seq("-J-Xss8M")

val http4sVersion = "0.21.0-M5"
val rhoVersion = "0.20.0-M1"
val circeVersion = "0.12.2"
val enumeratumVersion = "1.5.13"
val sttpVersion = "1.7.2"
val doobieVersion = "0.8.7"
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "com.typesafe" % "config" % "1.4.0",
  "com.github.pureconfig" %% "pureconfig" % "0.12.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "rho-swagger" % rhoVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % "1.5.22",
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion,
  "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "com.github.taycaldwell" % "riot-api-java" % "4.3.0",
  "io.github.resilience4j" % "resilience4j-ratelimiter" % "1.2.0"
)