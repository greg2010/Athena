name := "LolAssistant"
organization := "org.red"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "jitpack" at "https://jitpack.io"
/*
guardrailTasks in Compile := List(
  ScalaClient(file("swaggerspec-2.0.yml"), pkg="org.red.lolassistant.riotclient", framework = "http4s")
)
*/

enablePlugins(OpenAPIGeneratorPlugin)
openapiGeneratorName := "java"
openapiLibrary := Some("native")
openapiInputSpec := file("openapi-3.0.0.yml")
openapiGroupId := Some("org.red.lolassistant")
openapiArtifactId := Some("riotclient")
//openapiPackageName := Some("org.red.lolassistant.riotclient2")
openapiOutputDir := file("./riotclient")



val http4sVersion = "0.21.0-M6"
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
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % "1.5.22",
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion,
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % sttpVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "com.github.taycaldwell" % "riot-api-java" % "4.3.0"
)