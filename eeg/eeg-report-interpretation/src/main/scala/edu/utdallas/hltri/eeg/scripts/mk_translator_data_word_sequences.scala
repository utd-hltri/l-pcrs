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
import edu.utdallas.hltri.util.IntIdentifier
import resource._

import scala.collection.JavaConverters._

/**
  * Created by travis on 4/4/16.
  */
object mk_translator_data_word_sequences extends App {
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

  val vocab = new IntIdentifier[String]()
  vocab.add("_UNK")

   for (plog <- managed(ProgressLogger.fixedSize("Processing", corpus.getIdStream.count(), 10l, TimeUnit.SECONDS))) {
     var counter = 0
     var failed = 0;
     for (id <- corpus.getIdStream.iterator().asScala) {
       if (counter == 0) {
         descs = Files.newBufferedWriter(dst.resolve("train.tsv"))
       } else if (counter == devStart) {
         descs.close()
         descs = Files.newBufferedWriter(dst.resolve("dev.tsv"))
       } else if (counter == testStart) {
         descs.close()
         descs = Files.newBufferedWriter(dst.resolve("test.tsv"))
       }
       counter += 1

       for (doc <- managed(corpus.load(id))) {
           val intrClass = if (doc.get(EegNote.interpretation) == "ABNORMAL") 1 else 0

           val desc = getSections(doc, "description").flatMap(getTokens)
           val impr = getSections(doc, "impression").flatMap(getTokens)
//           if (desc.isEmpty || desc.length < 5) {
//             System.err.println(s"Failed to parse description from document $id with sections ${doc.get("regex-eeg", Section.TYPE).asScala.map(_(Section.title)).mkString("Sections: ", " | ", ".")}")
//           }
           val descWords = desc.map(t => vocab.getIDOrAdd(t.toString))
          val imprWords = impr.map(t => vocab.getIDOrAdd(t.toString))

          if (descWords.nonEmpty && imprWords.nonEmpty) {
            descs.append(s"${desc.mkString(" ")}\t${impr.mkString(" ")}\t$intrClass\n")
          } else {
            failed += 1
           }

         plog.update("finished doc #" + id + "[" + failed + " failed]")
       }
     }
     descs.close()
   }
 }
