name := "hltri-ml-core"

version := "0.2-SNAPSHOT"

organization := "edu.utdallas.hltri"

// If using CDH, also add Cloudera repo
resolvers += "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.4.1"

// Jackson Json library
libraryDependencies ++= Seq(
  //  "com.google.guava" % "guava" % "18.0.+",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.1"
)

// Mallet machine learning
libraryDependencies += "cc.mallet" % "mallet" % "2.0.7"

// Liblinear SVM
libraryDependencies += "de.bwaldvogel" % "liblinear" % "1.95"

// I dunno what these are
//  "org.nd4j" % "nd4j-api" % "0.0.3.5.5.1"
//  "org.nd4j" % "nd4j-jblas" % "0.0.3.5.5.1"