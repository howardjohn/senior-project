name := "core"
version := "0.0.1"
organization := "io.github.howardjohn"
scalaVersion := "2.12.4"

scalacOptions ++= Seq("-Ypartial-unification")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= {
  val Http4sVersion = "0.18.0-M8"
  val CirceVersion = "0.9.0"
  Seq(
    "io.github.howardjohn" %% "http4s-lambda" % "0.2.0-SNAPSHOT",
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  )
}
