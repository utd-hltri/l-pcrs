import sbt._

name := "mercury"

version := "0.2"

organization := "edu.utdallas.hltri"

scalaVersion := "2.11.8"

exportJars := true

val solrVersion = "5.2.0"
val jettyVersion = "9.2.10.v20150310"

//  libraryDependencies += "javax" % "javaee-api" % "7.0",
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"

libraryDependencies ++= Seq(
  "org.apache.velocity" % "velocity" % "1.7",
  "org.apache.velocity" % "velocity-tools" % "2.0"
)

// Solr search library (wraps lucene)
libraryDependencies += "org.apache.solr" % "solr-solrj" % solrVersion

// https://mvnrepository.com/artifact/com.h2database/h2
libraryDependencies += "com.h2database" % "h2" % "1.4.192"

// For parsing CSV files
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"

// For crazy idea error
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.8"

//enablePlugins(JettyPlugin)

lazy val mercury = (project in file(".")).dependsOn(util, scribe, medbase, inquire, `eeg-core`, `insight-wiki`)

lazy val `eeg-core` = ProjectRef(file("../eeg"), "eeg-core")

lazy val util = RootProject(file("../hltri-util"))

lazy val scribe = RootProject(file("../scribe"))

lazy val medbase = RootProject(file("../medbase"))

lazy val inquire = RootProject(file("../inquire"))

lazy val `insight-wiki` = ProjectRef(file("../insight/"), "insight-wiki")
