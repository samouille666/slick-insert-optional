organization := "com.affinytix"

name := "affinytix-config-api"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.h2database" % "h2" % "1.4.185" % Test
)

parallelExecution in Test := false
fork in Test := true

publishTo := Some(Resolver.file("file", new File("/var/lib/jenkins/.m2/repository/")))
