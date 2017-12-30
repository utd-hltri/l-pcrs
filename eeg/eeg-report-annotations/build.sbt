name := "eeg-report-annotations"

version := "0.1-SNAPSHOT"

organization := "edu.utdallas.hltri"

// https://mvnrepository.com/artifact/com.lexicalscope.jewelcli/jewelcli
libraryDependencies += "com.lexicalscope.jewelcli" % "jewelcli" % "0.8.9"

// http componenets for TfBoundaryAnnotator
libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpcore" % "4.4.1",
  "org.apache.httpcomponents" % "httpclient" % "4.4.1"
)