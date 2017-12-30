import sbt.Keys._

name := "hltri-util"

version := "1.0.1"

organization := "edu.utdallas.hltri"

// enable publishing to maven
publishMavenStyle := true

// do not append scala version to the generated artifacts
crossPaths := false

// do not add scala libraries as a dependency!
autoScalaLibrary := false

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

//scalacOptions += "-target:jvm-1.7"

//scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

//resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"


// Enable assertions
fork in run := true

javaOptions in run += "-ea"

// Google's general utility library
libraryDependencies ++= Seq(
  "com.google.guava"          % "guava"                 % "21.+", // Main library
  "com.google.code.findbugs"  % "jsr305"                % "3.0.+", // Type-annotations used by Guava
  "com.google.errorprone"  % "error_prone_annotations" % "2.0.15" // Guava annotations that spook sbt
)

val log4jVersion = "2.7"

// Logging backend for slf4j
libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-jcl" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-1.2-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-jul" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-web" % log4jVersion,
  "org.slf4j" % "slf4j-api" % "1.7.7"
)

// https://mvnrepository.com/artifact/com.lmax/disruptor
libraryDependencies += "com.lmax" % "disruptor" % "3.3.5"

exportJars := true

libraryDependencies += "com.googlecode.concurrent-trees" % "concurrent-trees" % "2.4.0"

// Fast collections
libraryDependencies += "net.sf.trove4j" % "trove4j" % "3.0.+"
libraryDependencies += "it.unimi.dsi" % "fastutil" % "7.0.12"

// Typesafe configuration framework
libraryDependencies += "com.typesafe" % "config" % "1.3.+"

// External java sorting
libraryDependencies +=  "com.google.code.externalsortinginjava" % "externalsortinginjava" % "0.1.+"

// Time stuff
libraryDependencies += "joda-time" % "joda-time" % "2.3"

// Simple CLI library
libraryDependencies += "info.picocli" % "picocli" % "1.0.0"

// For parsing CSV files
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"
