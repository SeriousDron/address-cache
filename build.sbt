name := "address-cache"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)
libraryDependencies += guice