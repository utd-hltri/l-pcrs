package edu.utdallas.hlt.trecmed.scripts

import collection.JavaConverters._
import com.google.common.collect.{HashMultimap, Multimap}
import edu.utdallas.hlt.text.{MedicalConcept, Sentence}
import edu.utdallas.hlt.trecmed.Visit
import edu.utdallas.hlt.trecmed.framework.App
import edu.utdallas.hlt.util.Config
import java.io.File

object AssertionSentenceExtractor {

  def main(args: Array[String]) {
    App.init(args : _*)
    val sentences : Multimap[String, Sentence] = HashMultimap.create()
    val isCorrectType = Set("PROBLEM", "TREATMENT", "TEST")
    val size = 100

    // iterate over documents
    for (visit <- new File(Config.get(classOf[Visit], "PATH").toString).list;
         report <- Visit.fromEncodedId(visit).getReports.asScala;
         sentence <- report.getDocument.getSub(classOf[Sentence]).asScala;
         concept <- sentence.getSub(classOf[MedicalConcept]).asScala if isCorrectType(concept.getType)) {
      val assertion = concept.getAssertionType
      if (sentences.get(assertion).size < size) {
        sentences.put(concept.getAssertionType, sentence)
      } else if (sentences.size >= 12 * size) {
        for (e <- sentences.entries().asScala)
          println(e.getKey + '\t' + e.getValue.asTokenizedString())
        return
      }
    }
  }
}
