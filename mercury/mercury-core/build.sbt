import sbt._

name := "mercury-core"

version := "0.1.2"

organization := "edu.utdallas.hltri"

// enable publishing to maven
publishMavenStyle := true

// do not append scala version to the generated artifacts
crossPaths := false

// do not add scala libraries as a dependency!
autoScalaLibrary := false

val solrVersion = "5.2.0"

// Connects STDIN to sbt during forked runs
connectInput in run := true

// Get rid of output prefix
outputStrategy in run := Some(StdoutOutput)

// When  using sbt-run, fork to a new process instead of running within the sbt process
fork in run := true

// Set default java options: enable assertions, set memory, set server mode
javaOptions ++= Seq("-ea", "-esa", "-Xmx14g", "-server")

// Set javac options
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked")

// Always export a .jar rather than .class files
exportJars := true

// Solr search library (wraps lucene)
libraryDependencies += "org.apache.solr" % "solr-solrj" % solrVersion

// https://mvnrepository.com/artifact/com.h2database/h2
libraryDependencies += "com.h2database" % "h2" % "1.4.192"

// For parsing CSV files
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"

lazy val self = Project(
  id = "mercury-core",
  base = file("."))
  .dependsOn(util, scribe, medbase, inquire, `eeg-core`,
    `insight-wiki`, `eeg-annotations`, `inquire-med`, `eeg-knowledge-graph`)

lazy val `eeg-core` = ProjectRef(file("../../eeg"), "eeg-core")

lazy val `eeg-annotations` = ProjectRef(file("../../eeg"), "eeg-annotations")

lazy val `eeg-knowledge-graph` = ProjectRef(file("../../eeg"), "eeg-knowledge-graph")

lazy val util = RootProject(file("../../hltri-util"))

lazy val scribe = RootProject(file("../../scribe"))

lazy val medbase = RootProject(file("../../medbase"))

lazy val inquire = RootProject(file("../../inquire"))

lazy val `inquire-med` = ProjectRef(file("../../inquire"), "inquire-med")

lazy val `insight-wiki` = ProjectRef(file("../../insight/"), "insight-wiki")