package edu.utdallas.hlt.trecmed.framework


import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.Date
import java.util.concurrent.ConcurrentSkipListSet

import edu.utdallas.hlt.medbase.snomed.{SNOMEDICD9Mapper, SNOMEDManager, SNOMEDRelationshipDirection, SNOMEDRelationshipType}
import edu.utdallas.hlt.medbase.umls.UMLSManager
import edu.utdallas.hlt.trecmed.analysis.TRECKeywordAnalyzer
import edu.utdallas.hlt.trecmed.evaluation.{Evaluator, HTMLGenerator}
import edu.utdallas.hlt.trecmed.expansion.PunctuationExpander
import edu.utdallas.hlt.trecmed.ranking._
import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher
import edu.utdallas.hlt.trecmed.{Topic, Visit}
import edu.utdallas.hltri.inquire.lucene.LuceneResult
import edu.utdallas.hltri.knowledge.WikiRedirectManager
import edu.utdallas.hltri.logging.Logging
import edu.utdallas.hltri.scribe.annotators.WordNetLemmatizer
import edu.utdallas.hltri.struct.Weighted
import edu.utdallas.hltri.util.Expansion
import org.rogach.scallop.ScallopConf
import org.slf4j.MDC

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable


/**
  *
  * @author travis
  */
trait TRECConf {
  this: ScallopConf =>

  import org.rogach.scallop._

  implicit val pathConverter: AnyRef with ValueConverter[Path] =
    singleArgConverter[Path](path => Paths.get(path))


  val outputPrefix: ScallopOption[Path] = opt[Path](
    name = "output-dir",
    noshort = true,
    argName = "PATH",
    default = Some(Paths.get("output")),
    descr = "directory path for all output"
  )
  val questions: ScallopOption[Path] = opt[Path](
    name = "questions", short = 'q', argName = "PATH", required = true, descr = "path to preprocessed input questions"
  )
  val runtag: ScallopOption[String] = opt[String](
    name = "runtag",
    short = 'r',
    argName = "TAG",
    required = true,
    descr = "name of the runtag to use for the current experiment (e.g. TREC_2012_001)"
  )
  val force: ScallopOption[Boolean] = opt[Boolean](
    name = "force",
    short = 'f',
    default = Some(false),
    descr = "replace existing output if it already exists (defaults to false)"
  )
  val evalQrels: ScallopOption[Path] = opt[Path](
    name = "qrels",
    noshort = true,
    argName = "PATH",
    required = false,
    descr = "location of official qrels to use for evaluation"
  )
  val evaluate: ScallopOption[Boolean] = toggle(
    name = "eval",
    default = Some(true),
    descrYes = "evaluate results against official qrels",
    descrNo = "skip evaluating results against official qrels"
  )

  val rerank: ScallopOption[Boolean] = toggle(
    name = "rerank", default = Some(true), descrYes = "re-rank results", descrNo = "skip re-ranking results"
  )

  val expand: ScallopOption[Boolean] = toggle(
    name = "expand", default = Some(true), descrYes = "do query expansion", descrNo = "skip doing query expansion"
  )

  val expanders: ScallopOption[List[String]] = opt[List[String]](
    name = "expanders",
    argName = "EXPANDER",
    default = Some(List("umls", "wiki", "snomed", "ngd")),
    descr = "list of expanders to use for reranking"
  )

  val rankers: ScallopOption[List[String]] = opt[List[String]](
    name = "rerankers", argName = "RERANKER", default = Some(
      List("admission", "age", "assertion", "discharge", "er", "gender", "neg")
    ), descr = "list of rereankers to use for reranking"
  )

  val output: ScallopOption[Path] = for {prefix <- outputPrefix
                                         runtag <- runtag} yield prefix.resolve(runtag)
}

object TREC {
  def main(args: Array[String]) {
    val pArgs = App.init(args: _*)
    val conf = new ScallopConf(pArgs) with TRECConf
    conf.afterInit()
    new TREC(conf).run()
  }
}

class TREC(conf: ScallopConf with TRECConf) extends Logging {
  implicit val config: ScallopConf with TRECConf = conf
  final val queries: mutable.Buffer[Topic] = mutable.Buffer[Topic]()
  private final val results = new TrieMap[Topic, mutable.Buffer[LuceneResult[Visit]]]()

  def prepare() {
    if (!conf.force() && (Files.exists(conf.output().resolve(".lock")) || Files.exists(
      conf.outputPrefix().resolve(
        conf.runtag() + ".xz"
      )
    ))) {
      log.error(s"Output already exists for runtag ${conf.runtag()} at ${conf.output()}")
      System.exit(1)
    }
    Files.createDirectories(conf.output())
    if (!Files.isWritable(conf.output())) {
      log.error(s"Unable to write to output path ${conf.output()}")
      System.exit(1)
    }

    Files.write(conf.output().resolve(".lock"), Array[Byte](), StandardOpenOption.CREATE)
    log.info(s"Beginning run ${conf.runtag()} at ${new Date}")

    log.info(s"${conf.summary}")
  }

  def forQueries(func: Topic => Any) {
    for (query <- queries.par) {
      MDC.put("Q", s"Q${query.getId} ")
      func(query)
    }
    MDC.clear()
  }

  def forQueriesSerial(func: Topic => Any) {
    for (query <- queries) {
      MDC.put("Q", s"Q${query.getId} ")
      func(query)
    }
    MDC.clear()
  }

  def getQueries: java.util.List[Topic] = queries.asJava


  def getResults: java.util.Map[Topic, java.util.List[LuceneResult[Visit]]] = results.mapValues(_.asJava).asJava

  lazy val getOutput: Path = conf.output()


  def read(): TREC = {
    prepare()
    log.info("------------------------------------------------------------------------")
    log.info("Reading queries")
    log.info("------------------------------------------------------------------------")
    queries ++= App.readQuestion(conf.questions().toString).asScala
    log.info(s"Read ${queries.size} queries from ${conf.questions().getFileName}")
    this
  }

  def analyze(): TREC = {
    log.info("------------------------------------------------------------------------")
    log.info("Analyzing queries")
    log.info("------------------------------------------------------------------------")
    val keywords: mutable.Set[String] = new ConcurrentSkipListSet[String]().asScala
    val analyzer = new TRECKeywordAnalyzer()
    forQueries {
      query => {
        analyzer.extract(query)
        keywords ++= query.getKeywords.asScala.map(_.asString())
        log.debug(s"Keywords: ${query.getKeywords}")
      }
    }
    val dest = conf.output().resolve("Keywords.txt")
    Files.write(dest, keywords.asJava, Charset.defaultCharset)
    log.debug(s"Saved ${keywords.size} Keywords to $dest")
    this
  }

  private def withDefault[V, S](it: S, v: S)(implicit ev1: S => TraversableOnce[V]): S = {
    if (it.nonEmpty) {
      it
    } else {
      v
    }
  }


  def expand(): TREC = {
    log.info("------------------------------------------------------------------------")
    log.info("Expanding queries")
    log.info("------------------------------------------------------------------------")
    val snomed = new SNOMEDManager()
    val icd9 = SNOMEDICD9Mapper.getDefault(snomed)
    val umls = new UMLSManager()
    val wiki = new WikiRedirectManager()
    val wordnet = new WordNetLemmatizer()
    val expanders: List[(CharSequence) => Expansion[Weighted[String]]] =
      withDefault(conf.expanders(), List("umls", "wiki", "snomed", "ngd"))
        .collect {
      case "umls" => (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("UMLS", 12, umls.expand(topic))
      case "wiki" => (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("Wiki", 10, wiki.expand(topic))
      case "snomed" => (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("SNOMED", 8, snomed.expandBy(
          SNOMEDRelationshipType.IS_A, 3, SNOMEDRelationshipDirection.CHILDREN).expand(topic))
    } ++ Seq(
      (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("WordNet::Lemma", 16, wordnet.expand(topic)),
      (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("ICD-9", 16, icd9.getICD9s(topic)),
      (topic: CharSequence) =>
        Expansion.newFixedWeightedExpansion("Punctuation", 16,
          PunctuationExpander.INSTANCE.expand(topic))
    )

    for (expander <- expanders) {
      forQueriesSerial { (topic: Topic) =>
        for (keyword <- topic.getKeywords.asScala) {
          keyword.addExpansion(expander.apply(keyword))
          for (subKeyword <- keyword.getSubKeywords.asScala) {
            subKeyword.addExpansion(expander.apply(subKeyword))
          }
        }
        Topic.mergeDuplicateKeywords(topic.getKeywords)
      }
    }
    snomed.close()
    umls.close()
    this
  }


  def retrieve(): TREC = {
    log.info("------------------------------------------------------------------------")
    log.info("Retrieving queries")
    log.info("------------------------------------------------------------------------")
    val engine = LuceneEMRSearcher.getVisitSearcher

    forQueries {
      query => {
        results.put(query, engine.search(query.asLuceneQuery(), 1000).getResults.asScala)
        log.info(f"Found ${results(query).size}%,d results")
      }
    }
    this
  }

  def rank(): TREC = {
    log.info("------------------------------------------------------------------------")
    log.info("(Re-)ranking results")
    log.info("------------------------------------------------------------------------")
    val reranker: VisitReranker = new VisitReranker({
      val rerankers = conf.rankers().collect {
        case "admission" => new AdmissionScorer()
        case "age" => new AgeScorer()
        case "assertion" => new AssertionScorer()
        case "discharge" => new DischargeScorer()
        case "er" => new EmergencyRoomScorer()
        case "gender" => new GenderScorer()
        case "neg" => new NegHedgeScorer()
      }
      new CompositeScorer(rerankers: _*)
    })

    forQueriesSerial {
      query => {
        results.put(query, reranker.rerank(query, results(query).asJavaCollection).asScala)
      }
    }
    this
  }

  def evaluate(): TREC = {
    log.info("------------------------------------------------------------------------")
    log.info("Generating QRels & evaluating")
    log.info("------------------------------------------------------------------------")
    val evaluator: Evaluator = new Evaluator.Builder(conf.output().resolve("qrels.txt"), conf.evalQrels()).writeQRels(
      getQueries, getResults, conf.runtag()
    ).build
    val measures = List("infAP", "bpref", "P_10")
    evaluator.runOfficialSampledEvaluations(conf.output().resolve("evaluation.txt"))
    forQueries {
      query => log.info(s"Official measures: ${evaluator.getOfficialMeasures(query.getId, measures.asJavaCollection)}")
    }
    for (measure <- measures) {
      log.info(s"Total ${measure.toUpperCase} is ${evaluator.getOfficialMeasure("all", measure)}")
    }
    log.info("------------------------------------------------------------------------")
    log.info("Outputting HTML")
    log.info("------------------------------------------------------------------------")
    val generator = new HTMLGenerator(getQueries, conf.output().resolve("html"), evaluator, measures.asJava)
    generator.generate()
    this
  }

  def run() {
    read()
    analyze()
    if (conf.expand()) expand()
    retrieve()
    if (conf.rerank()) rank()
    if (conf.evaluate()) evaluate()
  }
}
