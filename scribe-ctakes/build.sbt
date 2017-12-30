name := "scribe-ctakes"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.ctakes" % "ctakes-dictionary-lookup-fast" % "3.2.1"
)

lazy val ctakes = project in file(".") aggregate(scribe, util) dependsOn(scribe, util)

lazy val scribe = RootProject(file("../scribe"))

lazy val util = RootProject(file("../hltri-util"))