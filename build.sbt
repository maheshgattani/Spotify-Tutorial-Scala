import play.PlayScala

name := """test"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "1.1.0"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.1"

