name := "scribe-kirk"

version := "0.1.0"

fork := true


scalaVersion in ThisBuild := "2.11.2"

resolvers += "Artifactory" at "http://pnfs.hlt.utdallas.edu:8081/artifactory/repo/"

exportJars := true


libraryDependencies ++= Seq(
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "org.slf4j" % "jul-to-slf4j" % "1.7.7",
  "edu.utdallas.hltri.kirk" % "kirk-text" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-util" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-i2b2" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-genia_wrapper" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-kiwi" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "edu.utdallas.hltri.kirk" % "kirk-metamap_wrapper" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging"),
  "uk.ac.gate" % "gate-core" % "7.1",
  "edu.utdallas.hltri.kirk" % "kirk-heideltime_wrapper" % "1.0.0" exclude("log4j", "log4j") exclude("commons-logging", "commons-logging")
)


lazy val kirk = project in file(".") aggregate(scribe, util) dependsOn(scribe, util)

lazy val scribe = RootProject(file("../scribe"))

lazy val util = RootProject(file("../hltri-util"))
