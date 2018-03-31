lazy val commonSettings = Seq(
  organization := "io.github.howardjohn",
  scalaVersion := "2.12.4"
)

lazy val root = project
  .in(file("."))
  .aggregate(clientJS, clientJVM)

lazy val common = project
  .in(file("common"))
  .settings(commonSettings)
  .settings(
    moduleName := "common",
    libraryDependencies ++= {
      val CirceVersion = "0.9.2"
      Seq(
        "io.circe" %% "circe-core" % CirceVersion,
        "org.typelevel" %%% "cats-effect" % "0.10",
        "org.scalatest" %% "scalatest" % "3.0.4" % "test"
      )
    }
  )

lazy val backend = project
  .in(file("backend"))
  .settings(commonSettings)
  .settings(
    name := "backend",
    version := "0.0.1",
    moduleName := "backend",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= {
      val Http4sVersion = "0.18.0"
      val CirceVersion = "0.9.2"
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
  .dependsOn(common)

lazy val client = crossProject
  .in(file("client"))
  .settings(commonSettings)
  .settings(
    name := "client",
    moduleName := "client",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= {
      val CirceVersion = "0.9.2"
      val ScalaJSVersion = "1.0.0-M3"
      Seq(
        "org.typelevel" %%% "cats-effect" % "0.10",
        "org.scala-js" %% "scalajs-stubs" % ScalaJSVersion % "provided",
        "com.pepegar" %%% "hammock-core" % "0.8.1",
        "com.pepegar" %%% "hammock-circe" % "0.8.1",
        "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
      )
    }
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .jvmSettings()

lazy val clientJVM = client.jvm.dependsOn(common)
lazy val clientJS = client.js.dependsOn(common)
