lazy val projectCodename = "athena"

name := projectCodename
organization in ThisBuild := "org.kys"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.13.3"

// Projects
lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(backend)

lazy val backend = project
  .settings(name := "backend",
            settings,
            assemblySettings,
            libraryDependencies ++=
            Seq(dependencies.catsCore,
                dependencies.catsEffect,
                dependencies.typesafeConfig,
                dependencies.pureconfig,
                dependencies.scribe,
                dependencies.scribeSlf4j,
                dependencies.http4sDsl,
                dependencies.http4sBlazeServer,
                dependencies.http4sBlazeClient,
                dependencies.http4sCirce,
                dependencies.rhoSwagger,
                dependencies.circeGeneric,
                dependencies.circeGenericExtras,
                dependencies.circeParser,
                dependencies.circeLiteral,
                dependencies.enumeratum,
                dependencies.enumeratumCirce,
                dependencies.sttpCore,
                dependencies.sttpCirce,
                dependencies.sttpHttp4s,
                dependencies.scaffeine),
            javaOptions in Compile ++= Seq("-J-Xss8M"))

// Settings
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
  val catsVersion       = "2.2.0"
  val http4sVersion     = "0.21.8"
  val rhoVersion        = "0.21.0-RC1"
  val circeVersion      = "0.13.0"
  val enumeratumVersion = "1.6.1"
  val sttpVersion       = "3.0.0-RC3"
  val doobieVersion     = "0.8.7"

  val catsCore   = "org.typelevel" %% "cats-core" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsVersion

  val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
  val pureconfig     = "com.github.pureconfig" %% "pureconfig" % "0.14.0"

  val scribe      = "com.outr" %% "scribe" % "3.0.4"
  val scribeSlf4j = "com.outr" %% "scribe-slf4j" % "3.0.2"

  val http4sDsl         = "org.http4s" %% "http4s-dsl" % http4sVersion
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
  val http4sCirce       = "org.http4s" %% "http4s-circe" % http4sVersion
  val rhoSwagger        = "org.http4s" %% "rho-swagger" % rhoVersion

  val circeGeneric       = "io.circe" %% "circe-generic" % circeVersion
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
  val circeParser        = "io.circe" %% "circe-parser" % circeVersion
  val circeLiteral       = "io.circe" %% "circe-literal" % circeVersion

  val enumeratum      = "com.beachape" %% "enumeratum" % enumeratumVersion
  val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % "1.5.22"

  val sttpCore   = "com.softwaremill.sttp.client" %% "core" % sttpVersion
  val sttpCirce  = "com.softwaremill.sttp.client" %% "circe" % sttpVersion
  val sttpHttp4s = "com.softwaremill.sttp.client" %% "http4s-backend" % sttpVersion

  val scaffeine = "com.github.blemale" %% "scaffeine" % "4.0.2"
}
