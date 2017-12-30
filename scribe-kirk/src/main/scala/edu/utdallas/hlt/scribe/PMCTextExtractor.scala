//package edu.utdallas.hlt.scribe
//
//import edu.utdallas.hlt.scribe.text.gate.GateUtils
//import edu.utdallas.hltri.Resource
//import Resource.using
//import java.io.{FileWriter, BufferedWriter, File}
//import gate.{Corpus, Factory}
//import edu.utdallas.hlt.text._
//import edu.utdallas.hlt.i2b2.Concept
//import edu.utdallas.hlt.util.Config
//
//object PMCTextExtractor extends App {
//
//  val srcDir = new File(args(0))
//  val tarDir = new File(args(1))
//  val sdsName = args(2)
//
//  Config.init("edu.utdallas.hlt.scribe", args : _*)
//  GateUtils.init()
//
//  using(Factory.createDataStore("gate.persist.SerialDataStore", tarDir.toURI.toURL.toString)) { sds =>
//    val corpus = sds.adopt(Factory.newCorpus(sdsName), null).asInstanceOf[Corpus]
//    for (file <- srcDir.listFiles if file.getName.endsWith(".nxml")) {
//      val gDoc = Factory.newDocument(file.getAbsoluteFile.toURI.toURL)
//      val kDoc = new Document(gate.Utils.stringFor(gDoc, gDoc.getAnnotations("Original markups").getUnsafeAnnotations("body")))
//      kDoc.annotate(Token.STEM_TYPE)
//      kDoc.annotate(Concept.TYPE)
//
//      KirkConverter.copyAnnotationsFromKirkToGate(gDoc, kDoc)
//
//      corpus.synchronized{
//        corpus.add(gDoc)
//        corpus.sync()
//      }
//      Factory.deleteResource(gDoc)
//    }
//  }
//}