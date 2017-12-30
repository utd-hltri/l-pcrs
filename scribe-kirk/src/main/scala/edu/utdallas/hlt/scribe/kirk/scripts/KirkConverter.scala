//package edu.utdallas.hlt.scribe.kirk.scripts
//
//import edu.utdallas.hlt.genia_wrapper.GeniaTokenReaderAndWriter
//import edu.utdallas.hlt.scribe.gate.GateUtils
//import edu.utdallas.hlt.text.io.XMLDocumentReader
//import edu.utdallas.hlt.text.io.xml.{NegationSpanReaderAndWriter, HedgeSpanReaderAndWriter}
//import edu.utdallas.hlt.text.{Document => KDocument, Annotation => KAnnotation, _}
//import edu.utdallas.hlt.util.Place
//import edu.utdallas.hltri.Resource._
//import edu.utdallas.hltri.io.Implicits._
//import edu.utdallas.hltri.logging.Logging
//import gate.{Corpus, Factory, FeatureMap, Document => GDocument}
//import java.io.File
//import scala.collection.JavaConverters._
//import scala.io.Source
//import scala.concurrent.{ExecutionContext, Await, Future}
//import scala.concurrent.duration._
//import java.util.concurrent.ForkJoinPool
//import gate.persist.SerialDataStore
//import scala.util.Success
//
//
//object KirkConverter extends App with Logging {
//
//  def copyAnnotationsFromKirkToGate(gateDoc: GDocument, kirkDoc: KDocument) {
//    def copyTokens(feats: Token => FeatureMap) {
//      val tokens = kirkDoc.getTokens
//      log.trace(s"Copying ${tokens.size} tokens...")
//      for (tok <- tokens.asScala) {
//        gateDoc.getAnnotations.add(tok.getStartCharOffset, tok.getEndCharOffset, "Token", feats(tok))
//      }
//    }
//
//    def copyAnnotations[T <: KAnnotation](clazz: Class[T])(feats: T => FeatureMap = (_: T) => Factory.newFeatureMap(), filter: T => Boolean = (_: T) => true) {
//      log.trace(s"Copying ${kirkDoc.getAnnotations(clazz).size} ${clazz.getSimpleName} annotations...")
//      for (annot <- kirkDoc.getAnnotations(clazz).asScala if filter(annot)) {
//        gateDoc.getAnnotations.add(annot.getStartCharOffset, annot.getEndCharOffset, clazz.getSimpleName, feats(annot))
//      }
//    }
//
//    val isValidMCType = Set("PROBLEM", "TREATMENT", "TEST")
//
//    copyTokens(tok => gate.Utils.featureMap(
//      "category", tok.getPOS,
//      "stem", tok.getStem,
//      "string", tok.asRawString))
//    copyAnnotations(classOf[Sentence])()
//    copyAnnotations(classOf[MedicalConcept])(
//      mc => gate.Utils.featureMap(
//        "type", mc.getType,
//        "assertion", mc.getAssertionType),
//      filter = mc => isValidMCType(mc.getType))
//    copyAnnotations(classOf[Gender])()
//    copyAnnotations(classOf[NegationSpan])()
//    copyAnnotations(classOf[HedgeSpan])()
//
//
//  }
//
//  def convertFromKirkToGate(kirkDoc: KDocument): GDocument = {
//    val gateDoc = Factory.newDocument(kirkDoc.asRawString())
//
//
//    log.trace(s"Creating GATE document from ${kirkDoc.getFile.getName}...")
//    gateDoc.setName(kirkDoc.getDocumentID)
//
//    val feats = gateDoc.getFeatures
//    feats.put("docId", gateDoc.getName)
//    feats.put("filename", kirkDoc.getFile.getName)
//    feats.putAll(kirkDoc.getMetaDataMap)
//
//    copyAnnotationsFromKirkToGate(gateDoc, kirkDoc)
//
//    gateDoc
//  }
//
//  val kirkDir = new File(args(0))
//  val storeDir = new File(args(1))
//
//  GateUtils.init()
//
//  val reader = new XMLDocumentReader(new GeniaTokenReaderAndWriter(),
//    Gender.getXMLAnnotationReader,
//    new HedgeSpanReaderAndWriter(),
//    new NegationSpanReaderAndWriter())
//
//  val ex = new ForkJoinPool(Runtime.getRuntime.availableProcessors * 2, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
//
//  // Use our executor as the execution context implicitly passed to all async future computations
//  implicit val ec = ExecutionContext.fromExecutorService(ex)
//
//  val store = new SerialDataStore
//  store.setStorageDir(storeDir)
//  store.create()
//  store.open()
//
//  val futures = for (file <- kirkDir.tree.iterator if file.isFile && file.getName.endsWith(".xml.gz")) yield {
//    Future {
//      debug(s"Reading kirk document $file")
//      reader.read(Place.fromFile(file))
//    } map { kDoc =>
//      debug(s"Converting to gate document ${kDoc.getDocumentID}")
//      convertFromKirkToGate(kDoc)
//    } map { gDoc =>
//      debug(s"Synchronizing gate document ${gDoc.getName}")
//      gDoc.setDataStore(store)
//      gDoc.sync()
//      gate.Factory.deleteResource(gDoc)
//    }
//  }
//
//  info("Awaiting conversion...")
//  Await.ready(Future.sequence(futures), 1 day)
//
//  info("Done!")
//  store.close()
//}
