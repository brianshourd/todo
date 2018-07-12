name := "todo"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.6"

scalacOptions := Seq(
  "-deprecation",
  "-encoding", "utf8",
  "-unchecked",
  "-Xcheckinit",
  "-Xlint"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice // Guice dependency injection framework
libraryDependencies += jdbc // Java Database Connectivity API
libraryDependencies += evolutions // Database version management

// Anorm database connection library for Play
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.1"

// Postgresql JDBC driver
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.2.jre7"

// Test framework
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
