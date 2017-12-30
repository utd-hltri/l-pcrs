name := "scribe-mate"

version := "1.0.0-SNAPSHOT"

organization := "edu.utdallas.hltri"

//net.virtualvoid.sbt.graph.Plugin.graphSettings

resolvers += "artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"

fork in run := true

scalaVersion := "2.11.2"

//javaOptions += "-Xmx10g"

connectInput in run :=   true // Connects stdin to sbt during forked runs

outputStrategy in run :=   Some(StdoutOutput) // Get rid of output prefix

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "17.0",
  "com.googlecode.mate-tools" % "anna" % "3.5",
  "liblinear" % "liblinear-with-deps" % "1.51",
  "org.apache.opennlp" % "opennlp-maxent" % "3.0.3",
  "org.apache.opennlp" % "opennlp-tools" % "1.6.0",
  "edu.stanford.nlp" % "seg" % "1.0",
  "edu.stanford.nlp" % "stanford-parser" % "3.4",
  "se.lth.cs.srl" % "srl" % "1.0",
  "org.riedelcastro" % "whatswrong" % "0.2.4"
)

lazy val scribe_mate = project in file(".") dependsOn(util, scribe, medbase)

lazy val util = RootProject(file("../hltri-util"))

lazy val scribe = RootProject(file("../scribe"))

lazy val medbase = RootProject(file("../medbase"))
