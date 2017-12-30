import sbt.Keys._
import sbt.RootProject

name := "trecmed"

version := "2.2.1"

updateOptions in Global := updateOptions.in(Global).value.withCachedResolution(true)

javacOptions ++= Seq("-Xlint", "-g", "-source", "1.8", "-target", "1.8")

scalaVersion := "2.11.2"

fork in run := true

connectInput in run := true // Connects stdin to sbt during forked runs

outputStrategy in run := Some(StdoutOutput) // Get rid of output prefix

//resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

resolvers += "Artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"

scalacOptions ++= Seq("-deprecation")

//javacOptions ++= Seq("-Xlint", "-g")

// Command-line parsing
libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

// XML parser
libraryDependencies += "org.jdom" % "jdom2" % "2.0.5"

// HTML templating
libraryDependencies += "org.apache.velocity" % "velocity" % "1.7"

// CERN math library
libraryDependencies += "colt" % "colt" % "1.2.0"

// WordNet interface
libraryDependencies += "edu.mit" % "jwi" % "2.2.3"

// Ghetto legacy scala xml library
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

// Kirk's scary stuff
libraryDependencies ++= Seq(
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "edu.utdallas.hltri.kirk" % "kirk-text" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-util" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-i2b2" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging")
)

// Lucene IR library
libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "5.2.+",
  "org.apache.lucene" % "lucene-analyzers-common" % "5.2.+",
  "org.apache.lucene" % "lucene-queryparser" % "5.2.+",
  "org.apache.lucene" % "lucene-highlighter" % "5.2.+",
  "org.apache.lucene" % "lucene-benchmark" % "5.2.+"
)

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.0-rc3",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.6.0-rc3"
)

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.8-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

lazy val trecmed = project in file(".") dependsOn(scribe, medbase, util, `util-scala`, inquire, ml, `inquire-med`, `scribe-kirk`)

lazy val scribe = RootProject(file("../scribe"))

lazy val `scribe-kirk` = RootProject(file("../scribe-kirk"))

lazy val medbase = RootProject(file("../medbase"))

lazy val util = RootProject(file("../hltri-util"))

lazy val `util-scala` = RootProject(file("../hltri-util/hltri-util-scala"))

lazy val ml = ProjectRef(file("../hltri-ml"), "ml-core")

lazy val inquire = RootProject(file("../inquire"))

lazy val `inquire-med` = ProjectRef(file("../inquire"), "inquire-med")

lazy val insight = RootProject(file("../insight"))
