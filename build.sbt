lazy val commonSettings = Seq(
  organization := "io.github.howardjohn",
  scalaVersion := "2.12.4"
)

lazy val ScalaJSVersion = "1.0.0-M3"
lazy val CirceVersion = "0.9.2"

lazy val root = project
  .in(file("."))
  .aggregate(commonJVM, commonJS, backend, clientJS, clientJVM, frontend)

lazy val common = crossProject
  .in(file("common"))
  .settings(commonSettings)
  .settings(
    moduleName := "common",
    libraryDependencies ++= {
      Seq(
        "io.circe" %% "circe-core" % CirceVersion,
        "org.typelevel" %%% "cats-effect" % "0.10",
        "org.scalatest" %% "scalatest" % "3.0.4"
      )
    }
  )
.jvmSettings(
  libraryDependencies += "org.scala-js" %% "scalajs-stubs" % ScalaJSVersion % "provided"
)

lazy val commonJVM = common.jvm
lazy val commonJS = common.js

lazy val backend = project
  .in(file("backend"))
  .settings(commonSettings, dynamoTestSettings)
  .settings(
    name := "backend",
    version := "0.0.1",
    moduleName := "backend",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= {
      val Http4sVersion = "0.18.0"
      Seq(
        "io.github.howardjohn" %% "http4s-lambda" % "0.2.0-SNAPSHOT",
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
        "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
        "io.circe" %% "circe-parser" % CirceVersion,
        "org.slf4j" % "slf4j-simple" % "1.7.25" % Runtime,
        "com.gu" %% "scanamo" % "1.0.0-M3",
        "org.log4s" %% "log4s" % "1.4.0",
        "org.scalatest" %% "scalatest" % "3.0.4" % "test"
      )
    }
  )
  .dependsOn(commonJVM)

lazy val client = crossProject
  .in(file("client"))
  .settings(commonSettings)
  .settings(
    name := "client",
    moduleName := "client",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= {
      Seq(
        "org.typelevel" %%% "cats-effect" % "0.10",
        "org.scala-js" %% "scalajs-stubs" % ScalaJSVersion % "provided",
        "com.pepegar" %%% "hammock-core" % "0.8.3",
        "com.pepegar" %%% "hammock-circe" % "0.8.3",
        "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
      )
    }
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.4",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    scalaJSUseMainModuleInitializer := true
  )
  .jvmSettings()

lazy val clientJVM = client.jvm.dependsOn(commonJVM)
lazy val clientJS = client.js.dependsOn(commonJS)

lazy val frontend = project
  .in(file("frontend"))
  .dependsOn(clientJS)
  .dependsOn(commonJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings, webpackSettings)
  .settings(
    name := "frontend",
    version := "0.0.1",
    moduleName := "frontend",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq("-Ypartial-unification", "-P:scalajs:sjsDefinedByDefault"),
    addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full),
    libraryDependencies ++=
      Seq(
        "me.shadaj" %%% "slinky-core" % "0.3.2",
        "me.shadaj" %%% "slinky-web" % "0.3.2",
        "me.shadaj" %%% "slinky-hot" % "0.3.2",
        "me.shadaj" %%% "slinky-scalajsreact-interop" % "0.3.2"
      )
  )

val dynamoTestSettings = Seq(
  dynamoDBLocalDownloadDir := file(".dynamodb-local"),
  dynamoDBLocalPort := 8042,
  startDynamoDBLocal := startDynamoDBLocal.dependsOn(compile in Test).value,
  test in Test := (test in Test).dependsOn(startDynamoDBLocal).value,
  testOptions in Test += dynamoDBLocalTestCleanup.value,
  parallelExecution in Test := false
)

val webpackSettings = Seq(
  npmDependencies in Compile ++= Seq(
    "react" -> "16.3.1",
    "react-dom" -> "16.3.1",
    "react-proxy" -> "1.1.8",
    "bootstrap" -> "4.0.0",
    "reactstrap" -> "5.0.0",
    "react-router-dom" -> "4.2.2",
    "react-bootstrap-table" -> "4.3.1"
  ),
  npmDevDependencies in Compile ++= Seq(
    "file-loader" -> "1.1.11",
    "style-loader" -> "0.20.3",
    "css-loader" -> "0.28.11",
    "html-webpack-plugin" -> "3.2.0",
    "copy-webpack-plugin" -> "4.5.1"),
  version in webpack := "4.5.0",
  version in startWebpackDevServer := "3.1.3",
  webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack-fastopt.config.js"),
  webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack-opt.config.js"),
  webpackDevServerExtraArgs in fastOptJS := Seq("--inline", "--hot"),
  webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()
)
