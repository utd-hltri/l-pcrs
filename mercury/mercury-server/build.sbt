import sbt._

name := "mercury-server"

version := "0.1.3"

organization := "edu.utdallas.hltri"

// enable publishing to maven
publishMavenStyle := true

// do not append scala version to the generated artifacts
crossPaths := false

// do not add scala libraries as a dependency!
autoScalaLibrary := false

// Java EE servlet API
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"

// Java EE RESTful webservice API
libraryDependencies += "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1"

libraryDependencies ++= Seq(
  "org.apache.velocity" % "velocity" % "1.7",
  "org.apache.velocity" % "velocity-tools" % "2.0"
)

// DoS Filter https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-servlets
libraryDependencies += "org.eclipse.jetty" % "jetty-servlets" % "7.6.9.v20130131"

updateOptions in Global := updateOptions.in(Global).value.withCachedResolution(true)

// Connects STDIN to sbt during forked runs
connectInput in run := true

// Get rid of output prefix
outputStrategy in run := Some(StdoutOutput)

// When  using sbt-run, fork to a new process instead of running within the sbt process
fork in run := true

// Set default java options: enable assertions, set memory, set server mode
javaOptions ++= Seq("-ea", "-esa", "-Xmx14g", "-server")

// set mercury.conf as the main config file
//javaOptions += "-Dconfig.file=/home/hermes/hltri-shared/mercury/mercury-server/src/main/resources/mercury.conf"

// Set javac options
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked")

// Always export a .jar rather than .class files
exportJars := true

//addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "2.1.0")
enablePlugins(JettyPlugin)

//lazy val `eeg-core` = ProjectRef(file("../../eeg"), "eeg-core")
//
//lazy val `eeg-report-annotations` = ProjectRef(file("../../eeg"), "report-annotations")
//
//lazy val util = RootProject(file("../../hltri-util"))
//
//lazy val scribe = RootProject(file("../../scribe"))
//
//lazy val medbase = RootProject(file("../../medbase"))
//
//lazy val inquire = RootProject(file("../../inquire"))
//
//lazy val `inquire-med` = ProjectRef(file("../../inquire"), "inquire-med")
//
//lazy val `insight-wiki` = ProjectRef(file("../../insight/"), "insight-wiki")

lazy val core = RootProject(file("../mercury-core"))

lazy val server = Project(
  id = "mercury-server",
  base = file("."))
  .dependsOn(core)
