package edu.utdallas.hltri.eeg.graph

import java.io.IOException

import edu.utdallas.hltri.conf.Config
import org.apache.spark.{Logging, SparkContext}

/**
 * Created by travis on 3/2/16.
 */
object mkgraph extends App with Logging {

  val inputFiles = args.slice(0, args.length - 1)
  val graphFile = args.last

  try {
    val hdfs = org.apache.hadoop.fs.FileSystem.get(
      new java.net.URI("hdfs://pnfs:9000"),
      new org.apache.hadoop.conf.Configuration())

    hdfs.delete(new org.apache.hadoop.fs.Path(graphFile + ".occurrences.tsv"), true);
    hdfs.delete(new org.apache.hadoop.fs.Path(graphFile + ".cooccurrences.tsv"), true)
  } catch {
    case _ : IOException => { /* Do nothing */ }
  }

  private val config: Config = Config.load("eeg")


  val sc = new SparkContext("spark://pnfs.hlt.utdallas.edu:7077", "eeg:mkgraph")

  val concepts = sc.textFile(inputFiles.mkString(",")).distinct().mapPartitions(
    lines =>
      lines.map{
        line =>
          val delim = line.indexOf('\t')
          line.substring(0, delim) -> line.substring(delim + 1)
      }
  )

  val occs = concepts.map(x => x._2 -> 1)
    .reduceByKey(_ + _)
    .saveAsTextFile(graphFile + ".occurrences.tsv")

  val cooccs = concepts.join(concepts).map((x: (String, (String, String))) => x._2 -> 1).reduceByKey(_ + _)
    .saveAsTextFile(graphFile + ".cooccurrences.tsv")
}
