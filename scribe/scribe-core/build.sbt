name := "scribe-core"

version := "0.1-SNAPSHOT"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

exportJars := true

// https://mvnrepository.com/artifact/org.reflections/reflections
libraryDependencies += "org.reflections" % "reflections" % "0.9.10"

// https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
libraryDependencies += "it.unimi.dsi" % "fastutil" % "7.0.12"

lazy val scribeCore = project in file(".") /* aggregate(util) */ //dependsOn(util)

//lazy val util = RootProject(file("../../hltri-util"))
