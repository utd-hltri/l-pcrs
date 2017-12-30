name := "hltri-ml"

organization := "edu.utdallas.hltri"

lazy val resolverSettings = Seq(
  resolvers += "artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"
)

lazy val commonSettings = resolverSettings ++ Seq(
  // Use latest scala version
  scalaVersion := "2.11.8",
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
  // Disable putting scala version in jar's name
  crossPaths := false,
  // Use consistent version
  version := "0.2-SNAPSHOT"
)

lazy val `hltri-ml` = (project in file("."))
  .aggregate(`ml-api`, `ml-core`)
  .settings(commonSettings)

lazy val `ml-api` = (project in file("hltri-ml-api"))
  .dependsOn(util)
  .settings(commonSettings)

lazy val `ml-core` = (project in file("hltri-ml-core"))
  .dependsOn(`ml-api`, util)
  .settings(commonSettings)

lazy val util = RootProject(file("../hltri-util"))
