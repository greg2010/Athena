lazy val projectCodename = "Athena"

name := projectCodename
organization in ThisBuild := "org.kys"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.13.1"

// Projects
lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(common, backend)

lazy val common = project
  .settings(name := "common", settings)
  .disablePlugins(AssemblyPlugin)

lazy val backend = project
  .settings(name := "backend",
            settings,
            assemblySettings,
            libraryDependencies ++=
            Seq(dependencies.catsCore,
                dependencies.catsEffect,
                dependencies.typesafeConfig,
                dependencies.pureconfig,
                dependencies.logbackClassic,
                dependencies.scalaLogging,
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
                dependencies.sttpCats,
                dependencies.resilience4j,
                dependencies.scaffeine),
            javaOptions in Compile ++= Seq("-J-Xss8M"))
  .dependsOn(common)

lazy val frontend = project
  .settings(name := "frontend",
            settings,
            assemblySettings,
            libraryDependencies ++= Seq(dependencies.scalaJsLogging,
                                        dependencies.scalaJsDom,
                                        dependencies.scalaJsBindingHtml,
                                        dependencies.scalaJsBindingFuture,
                                        dependencies.scalaJsSttpClient,
                                        //dependencies.scalaJsSttpCirce,
                                        dependencies.scalaJscirceGeneric,
                                        dependencies.scalaJscirceGenericExtras,
                                        dependencies.scalaJscirceParser,
                                        dependencies.scalaJscirceLiteral),
            scalaJSUseMainModuleInitializer := true)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common)

// Settings
lazy val compilerOptions = Seq("-unchecked", "-feature", "-deprecation", "-Wunused:imports", "-Ymacro-annotations",
                               "-encoding", "utf8")

lazy val commonSettings = Seq(scalacOptions ++= compilerOptions, resolvers += "jitpack" at "https://jitpack.io")

lazy val wartremoverSettings = Seq(wartremoverWarnings in(Compile, compile) ++= Warts.unsafe.filterNot { w =>
  w == Wart.Any || w == Wart.Nothing || w == Wart.DefaultArguments || w == Wart.StringPlusAny ||
  w == Wart.NonUnitStatements
})

lazy val assemblySettings = Seq(assemblyJarName in assembly := projectCodename + "-" + name.value + ".jar",
                                assemblyMergeStrategy in assembly :=
                                { case PathList("META-INF", xs @ _*) => MergeStrategy.discard
                                case "application.conf" => MergeStrategy.concat
                                case x => val oldStrategy = (assemblyMergeStrategy in assembly).value
                                  oldStrategy(x)
                                })

lazy val settings = commonSettings ++ wartremoverSettings

// Dependencies repository
lazy val dependencies = new {
  val catsVersion       = "2.0.0"
  val http4sVersion     = "0.21.0-M5"
  val rhoVersion        = "0.20.0-M1"
  val circeVersion      = "0.12.1"
  val enumeratumVersion = "1.5.13"
  val sttpVersion       = "2.0.0-RC6"
  val doobieVersion     = "0.8.7"

  val catsCore   = "org.typelevel" %% "cats-core" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsVersion

  val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
  val pureconfig     = "com.github.pureconfig" %% "pureconfig" % "0.12.2"

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

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

  val sttpCore  = "com.softwaremill.sttp.client" %% "core" % sttpVersion
  val sttpCirce = "com.softwaremill.sttp.client" %% "circe" % sttpVersion
  val sttpCats  = "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % sttpVersion

  val resilience4j = "io.github.resilience4j" % "resilience4j-ratelimiter" % "1.2.0"

  val scaffeine = "com.github.blemale" %% "scaffeine" % "3.1.0"

  val scalaJsLogging = "org.scala-js" %%%! "scalajs-java-logging" % "0.1.6"

  val scalaJsDom           = "org.scala-js" %%%! "scalajs-dom" % "0.9.7"
  val scalaJsBindingHtml   = "org.lrng.binding" %%%! "html" % "1.0.2+97-13b1908e"
  val scalaJsBindingFuture = "com.thoughtworks.binding" %%%! "futurebinding" % "12.0.0-M0"

  val scalaJsSttpClient = "com.softwaremill.sttp.client" %%%! "core" % sttpVersion
  //val scalaJsSttpCirce  = "com.softwaremill.sttp.client" %%%! "circe_sjs" % sttpVersion

  val scalaJscirceGeneric       = "io.circe" %%%! "circe-generic" % circeVersion
  val scalaJscirceGenericExtras = "io.circe" %%%! "circe-generic-extras" % circeVersion
  val scalaJscirceParser        = "io.circe" %%%! "circe-parser" % circeVersion
  val scalaJscirceLiteral       = "io.circe" %%%! "circe-literal" % circeVersion

}
