lazy val projectCodename = "athena"

name := projectCodename
organization in ThisBuild := "org.kys"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.13.4"

// Projects
lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(common.jvm, common.js, backend, frontend)

lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(name := "common",
            settings,
            libraryDependencies ++= dependencies.common.value)
  .disablePlugins(AssemblyPlugin)

lazy val backend = project
  .settings(name := "backend",
            settings,
            assemblySettings,
            libraryDependencies ++= dependencies.jvm.value,
            javaOptions in Compile ++= Seq("-J-Xss8M"))
  .dependsOn(common.jvm)

lazy val frontend = project
  .settings(name := "frontend",
            settings,
            libraryDependencies ++= dependencies.js.value,
            scalaJSUseMainModuleInitializer := true)
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common.js)

// Settingssb
lazy val compilerOptions = Seq("-unchecked", "-feature", "-deprecation", "-Wunused:imports", "-Ymacro-annotations",
                               "-encoding", "utf8")

lazy val commonSettings = Seq(scalacOptions ++= compilerOptions, resolvers += "jitpack" at "https://jitpack.io")

lazy val wartremoverSettings = Seq(wartremoverWarnings in(Compile, compile) ++= Warts.unsafe.filterNot { w =>
  w == Wart.Any || w == Wart.Nothing || w == Wart.DefaultArguments || w == Wart.StringPlusAny ||
  w == Wart.NonUnitStatements
})

lazy val assemblySettings = Seq(assemblyJarName in assembly := projectCodename + "-" + name.value + ".jar",
                                assemblyMergeStrategy in assembly := {
                                  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
                                  case "module-info.class" => MergeStrategy.discard
                                  case "application.conf" => MergeStrategy.concat
                                  case x =>
                                    val oldStrategy = (assemblyMergeStrategy in assembly).value
                                    oldStrategy(x)
                                })

lazy val settings = commonSettings ++ wartremoverSettings

// Dependencies repository
lazy val dependencies = new {
  val catsVersion       = "2.3.0"
  val http4sVersion     = "0.21.13"
  val rhoVersion        = "0.21.0-RC1"
  val circeVersion      = "0.13.0"
  val enumeratumVersion = "1.6.1"
  val sttpVersion       = "3.0.0-RC11"
  val doobieVersion     = "0.8.7"
  val scribeVersion     = "3.1.7"

  val laminarVersion   = "0.11.0"
  val airstreamVersion = "0.11.1"

  val zioVersion     = "1.0.3"
  val zioCatsVersion = "2.2.0.1"

  val tapirVersion = "0.17.1"

  val common = Def.setting(Seq(
    "com.beachape" %%% "enumeratum" % enumeratumVersion,
    "com.beachape" %%% "enumeratum-circe" % "1.6.1",
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-generic-extras" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "io.circe" %%% "circe-literal" % circeVersion,
    "com.outr" %%% "scribe" % scribeVersion,
    "com.softwaremill.sttp.client3" %%% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %%% "circe" % sttpVersion,
    "dev.zio" %%% "zio" % zioVersion,
    "dev.zio" %%% "zio-interop-cats" % zioCatsVersion,
    "com.softwaremill.sttp.tapir" %%% "tapir-core" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion))

  val jvm = Def.setting(common.value ++ Seq(
    "com.typesafe" % "config" % "1.4.1",
    "com.github.pureconfig" %% "pureconfig" % "0.14.0",
    "com.outr" %% "scribe-slf4j" % scribeVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "com.softwaremill.sttp.client3" %% "http4s-backend" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % sttpVersion,
    "com.github.blemale" %% "scaffeine" % "4.0.2",
    "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % tapirVersion,
    "dev.zio" %% "zio-macros" % zioVersion
    ))

  val js = Def.setting(common.value ++ Seq(
    "org.typelevel" %%% "cats-core" % catsVersion,
    "org.typelevel" %%% "cats-effect" % catsVersion,
    "com.raquo" %%% "laminar" % laminarVersion,
    "com.raquo" %%% "airstream" % airstreamVersion,
    "com.raquo" %%% "waypoint" % "0.2.0"
    ))
}
