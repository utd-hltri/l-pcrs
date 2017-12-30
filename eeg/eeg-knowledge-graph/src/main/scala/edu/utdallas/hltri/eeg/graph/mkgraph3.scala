package edu.utdallas.hltri.eeg.graph

import java.io.IOException
import java.nio.file.{StandardOpenOption, Paths, Files}

import com.google.common.base.CharMatcher
import edu.utdallas.hltri.conf.Config
import edu.utdallas.hltri.eeg.annotation.EegActivity
import edu.utdallas.hltri.scribe.io.JsonCorpus
import edu.utdallas.hltri.scribe.text.annotation._
import edu.utdallas.hltri.scribe.text.{Attribute, BaseDocument}
import org.apache.spark.{Logging, SparkContext}

import scala.collection.GenTraversableOnce
import scala.collection.JavaConverters._
import scala.compat.java8.FunctionConverters._

import resource._

/**
  * Created by travis on 3/2/16.
  */
object mkgraph3 extends App with Logging {
  val fileName = args(0)

  private val config: Config = Config.load("eeg")
  val eegs = JsonCorpus.at[BaseDocument](config.getString("json-path"))
    .annotationSets("fs1", "regex-eeg")
    .tiered
    .build

  val annotations: Seq[(String, AnnotationType[_ >: EegActivity with Event <: AbstractAnnotation[_ >: EegActivity with Event]])] = Seq(
    ("fs1", EegActivity.TYPE),
    ("fs1", Event.TYPE)
  )

  def getValues(value: String): Array[String] = {
    if (value == "NONE") Array("ANY")
    else Array(value, "ANY")
  }

  def encode(annotation: Annotation[_]): GenTraversableOnce[String] = {
    val sections = annotation.getCovering("regex-eeg", Section.TYPE)
    val sect = if (sections.isEmpty) {
      "NONE"
    } else {
      CharMatcher
        .WHITESPACE
        .collapseFrom(sections.get(0).get(Section.title).toLowerCase, '_')
    }
    annotation match {
      case _: EegActivity =>
        val a: EegActivity = annotation.asInstanceOf[EegActivity]
        for (band <- getValues(a(EegActivity.band));
             dist <- getValues(a(EegActivity.dispersal));
             freq <- getValues(a(EegActivity.recurrence));
             hemi <- getValues(a(EegActivity.hemisphere));
             loct <- getValues(a(EegActivity.location));
             magn <- getValues(a(EegActivity.magnitude));
             wavf <- getValues(a(EegActivity.morphology)))
          yield s"${a.toCollapsedLowercase('_')}|$band|$dist|$freq|$hemi|$loct|$magn|$wavf|$sect"
      case _: Event =>
        val e: Event = annotation.asInstanceOf[Event]
        Seq(s"${e.toCollapsedLowercase('_')}|${e(Event.`type`)}|$sect")
    }
  }

  for (writer <- managed(Files.newBufferedWriter(Paths.get(s"$fileName.tsv")))) {
    eegs.forEachDocument(asJavaConsumer { doc =>
      val activites = doc.get("fs1", EegActivity.TYPE).iterator().asScala.flatMap(x => encode(x)).map(annotation => (doc.get(BaseDocument.id), annotation.toString))
      val events = doc.get("fs1", Event.TYPE).iterator().asScala.flatMap(x => encode(x)).map(annotation => (doc.get(BaseDocument.id), annotation.toString))
      for ((docid, annotation) <- (activites ++ events).toSet) {
        writer.append(s"${doc.get(BaseDocument.id)}\t$annotation\n")
      }
    })
  }
}
