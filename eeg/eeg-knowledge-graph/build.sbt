import sbt.complete.DefaultParsers._
import scala.sys.process._

name := "eeg-graph"

version := "0.1-SNAPSHOT"

organization := "edu.utdallas.hltri"

scalaVersion := "2.11.8"

connectInput in run := true

outputStrategy in run := Some(StdoutOutput)

javaOptions += "-ea"

fork in run := true

// json parser
libraryDependencies += "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.4"

val hadoopVersion = "2.5.0"
val sparkVersion = "1.4.1"

// If using CDH, also add Cloudera repo
resolvers += "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

libraryDependencies += "org.apache.hadoop" % "hadoop-client" % hadoopVersion exclude ("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion exclude ("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.apache.spark" %% "spark-graphx" % sparkVersion exclude ("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0"

// Fancy scala resource management
libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.4"

lazy val `spark-main` = inputKey[Unit]("Executes the main method on the given class against the Spark cluster")

exportJars := true

`spark-main` := {
  var classPath = (fullClasspath in Runtime value) map (e => e.data) filter (e => e.getName.endsWith("jar"))
  classPath.foreach( f => stringToProcess(s"chmod o+r $f").! )

  val (art, file) =  (packagedArtifact in (Compile, packageBin)).value
  val mainJar = file.getAbsolutePath

  val sparkHome = sys.env("SPARK_HOME")
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  val mainClass = args.head
  val mainArgs = args.tail

  val x = s"$sparkHome/bin/spark-submit --master spark://pnfs.hlt.utdallas.edu:7077 --deploy-mode client --jars ${classPath.mkString(",local:")} --class $mainClass $mainJar ${mainArgs.mkString(" ")}"
  println(x)
  x!
}

`spark-main` := `spark-main`.dependsOn(packageBin in Compile)