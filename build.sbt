name := "akka-stream-experiments"

version := "1.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  //"com.typesafe.akka" %% "akka-actor" % "2.3.9" % "compile",
  //"com.typesafe.akka" %% "akka-http-experimental" % "1.0-M2" % "compile",
  //"com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M2" % "compile",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M3" % "compile",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test"
)