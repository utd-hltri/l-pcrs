name := "eeg-interpretation"

version := "0.1-SNAPSHOT"

organization := "edu.utdallas.hltri"

scalaVersion := "2.11.8"

connectInput in run := true

outputStrategy in run := Some(StdoutOutput)

javaOptions += "-ea"

fork in run := true