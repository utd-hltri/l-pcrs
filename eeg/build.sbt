import sbt.Keys.publishMavenStyle

name := "eeg"

version := "0.1-SNAPSHOT"

organization := "edu.utdallas.hltri"

lazy val resolverSettings = Seq(
  resolvers += "artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"
)

lazy val commonSettings = resolverSettings ++ Seq(
  // Connects STDIN to sbt during forked runs
  connectInput in run := true,
  // Get rid of output prefix
  outputStrategy in run := Some(StdoutOutput),
  // When  using sbt-run, fork to a new process instead of running within the sbt process
  fork in run := true,
  // Set default java options: enable assertions, set memory, set server mode
  javaOptions ++= Seq("-ea", "-esa", "-Xmx14g", "-server"),
  // Set javac options
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  // Always export a .jar rather than .class files
  exportJars := true,

  publishMavenStyle := true,
  // do not append scala version to the generated artifacts
  crossPaths := false,
  // do not add scala libraries as a dependency!
  autoScalaLibrary := false
)

lazy val `eeg` = (project in file("."))
  .aggregate(`eeg-knowledge-graph`, `eeg-annotations`, `eeg-core`, `eeg-interpretation`)
  .settings(commonSettings)

lazy val `eeg-core` = (project in file("eeg-core"))
  .dependsOn(`ml-api`, scribe, util)
  .settings(commonSettings)

lazy val `eeg-knowledge-graph` = (project in file("eeg-knowledge-graph"))
  .dependsOn(`eeg-core`, medbase, scribe, `util-scala`, `eeg-annotations`)
  .settings(commonSettings)

lazy val `eeg-annotations` = (project in file("eeg-report-annotations"))
  .dependsOn(`eeg-core`, `ml-core`, `ml-api`, medbase)
  .settings(commonSettings)

lazy val `eeg-interpretation` = (project in file("eeg-report-interpretation"))
  .dependsOn(`eeg-core`, `util-scala`)
  .settings(commonSettings)

lazy val scribe = RootProject(file("../scribe"))

lazy val util = RootProject(file("../hltri-util"))

lazy val `ml-api` = ProjectRef(file("../hltri-ml/"), "ml-api")

lazy val `ml-core` = ProjectRef(file("../hltri-ml/"), "ml-core")

lazy val `util-scala` = RootProject(file("../hltri-util/hltri-util-scala"))

lazy val medbase = RootProject(file("../medbase"))


