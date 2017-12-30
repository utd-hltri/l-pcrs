package edu.utdallas.hltri.eeg.scripts

import java.io.BufferedWriter
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import com.google.common.base.CharMatcher
import edu.utdallas.hltri.eeg.EegNote
import edu.utdallas.hltri.framework.ProgressLogger
import edu.utdallas.hltri.scribe.io.JsonCorpus
import edu.utdallas.hltri.scribe.text.Document
import edu.utdallas.hltri.scribe.text.annotation.{Section, Token}
import resource._

import scala.collection.JavaConverters._
/**
 * Created by travis on 4/4/16.
 */
object mk_translator_data extends App {
  val src = Paths.get("/shared/aifiles/disk1/travis/data/corpora/???")
  val dst = Paths.get(args(0))
  Files.createDirectories(dst)

//  val corpus0 = JsonCorpus.at[BaseDocument]("/shared/aifiles/disk1/travis/data/corpora/tuh_eeg/v0.6.0_json").annotationSets(/*"genia",*/).tiered.build
  val corpus = JsonCorpus.at[EegNote]("/shared/aifiles/disk1/travis/data/corpora/tuh_eeg/v0.6.0_json").annotationSets("genia", "regex-eeg").tiered.build
  val docs = corpus.getIdStream.count()

//  for (plog <- managed(ProgressLogger.fixedSize("Processing", corpus.getIdStream.count(), 10l, TimeUnit.SECONDS))) {
//    val sectionAnnotator = new RegExEegAnnotator.Builder[BaseDocument]().annotateSections().ignoreTitles("HR", "BP").clear().build()
//    corpus0.forEachDocument(asJavaConsumer { (d: Document[BaseDocument]) =>
//      sectionAnnotator.annotate(d)
//      d.setCorpus(corpus)
//      d.sync()
//      plog.update(s"Finished #${d(BaseDocument.id)}")
//    })
//  }

//  System.exit(0)

  val matcher = CharMatcher.WHITESPACE

  def getTokens(s: Section): Seq[Token] = {
    val tokens = s.getContained("genia", Token.TYPE).asScala //.toBuffer
//    val indx = tokens.indexWhere(_.toString == ":")
//    tokens.trimStart(indx + 1)
    tokens
  }

  def getSections(doc: Document[_], n: String): Seq[Section] = {
    doc.get("regex-eeg", Section.TYPE).asScala.filter(s => s(Section.title).toLowerCase.contains(n))
  }


  val devStart = (docs / 5) * 3
  val testStart = (docs / 5) * 4


//  for (descs <- managed(Files.newBufferedWriter(dst.resolve("descriptions.tsv")));
//       intrs <- managed(Files.newBufferedWriter(dst.resolve("interpretations.tsv")))) {
  var descs: BufferedWriter = _
  var intrs: BufferedWriter = _
  for (plog <- managed(ProgressLogger.fixedSize("Processing", corpus.getIdStream.count(), 10l, TimeUnit.SECONDS))) {
    var counter = 0
    var failed = 0;
    for (id <- corpus.getIdStream.iterator().asScala) {
      if (counter == 0) {
        descs = Files.newBufferedWriter(dst.resolve("train.desc.tsv"))
        intrs = Files.newBufferedWriter(dst.resolve("train.impr.tsv"))
      } else if (counter == devStart) {
        descs.close()
        intrs.close()
        descs = Files.newBufferedWriter(dst.resolve("dev.desc.tsv"))
        intrs = Files.newBufferedWriter(dst.resolve("dev.impr.tsv"))
      } else if (counter == testStart) {
        descs.close()
        intrs.close()
        descs = Files.newBufferedWriter(dst.resolve("test.desc.tsv"))
        intrs = Files.newBufferedWriter(dst.resolve("test.impr.tsv"))
      }
      counter += 1

      var intrv: String = null
      var descv: String = null
      for (doc <- managed(corpus.load(id))) {
        {
          val intrClass = doc.get(EegNote.interpretation)
          val intrText = getSections(doc, "impression").flatMap(getTokens)
          if (intrText.isEmpty || intrText.length < 2) {
            System.err.println(s"Failed to parse impression from document $id with sections ${doc.get("regex-eeg", Section.TYPE).asScala.map(_(Section.title)).mkString("Sections: ", " | ", ".")}")
          } else {
            intrv = s"$id\t$intrClass\t${intrText.mkString(" ")}\n"
          }
        }
        {
          val desc = getSections(doc, "description").flatMap(getTokens)
          if (desc.isEmpty || desc.length < 5) {
            System.err.println(s"Failed to parse description from document $id with sections ${doc.get("regex-eeg", Section.TYPE).asScala.map(_(Section.title)).mkString("Sections: ", " | ", ".")}")
          }
          descv = (for (t <- desc ) yield {
            val feats = Seq(t)
            s"$id\t${feats.mkString("\t")}\n"
          }).mkString
        }
        if (intrv != null && descv != null) {
          intrs.append(intrv)
          descs.append(descv)
        } else {
          failed += 1
        }

        plog.update("finished doc #" + id + "[" + failed + " failed]")
      }
    }
    descs.close()
    intrs.close()
  }
}
