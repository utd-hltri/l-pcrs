name := "scribe"

organization := "edu.utdallas.hltri"

version := "0.3.1"

// enable publishing to maven
publishMavenStyle := true

// do not append scala version to the generated artifacts
crossPaths := false

// do not add scala libraries as a dependency!
autoScalaLibrary := false

fork in run := true

connectInput in run := true // Connects stdin to sbt during forked runs

outputStrategy in run := Some(StdoutOutput) // Get rid of output prefix

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

javacOptions ++= Seq("-Xlint")

exportJars := true

resolvers ++= Seq(
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"
)

// Graph algorithm library
libraryDependencies ++= Seq(
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-algorithms" % "2.0.1"
)

// WordNet interface
libraryDependencies += "edu.mit" % "jwi" % "2.2.3"

// JDom for SemaforFrameNetAnnotator
libraryDependencies += "org.jdom" % "jdom2" % "2.0.5"

libraryDependencies ++= Seq(
  "uk.ac.gate"        % "gate-core"         % "8.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.stanford.nlp"  % "stanford-corenlp"  % "3.6.0",
  "edu.stanford.nlp"  % "stanford-corenlp" % "3.6.0" classifier "models",
  "com.google.code.findbugs" % "jsr305" % "3.0.+",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.1"
)

// Compression stuff
libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % "1.9",
  "org.tukaani" % "xz" % "1.5"
)

lazy val scribe = project in file(".") /* aggregate(util) */ dependsOn util

lazy val util = RootProject(file("../hltri-util"))
