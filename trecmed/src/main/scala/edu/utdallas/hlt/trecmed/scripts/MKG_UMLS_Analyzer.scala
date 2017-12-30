package edu.utdallas.hlt.trecmed.scripts

import com.google.common.base.Splitter
import edu.utdallas.hlt.medbase.umls.UMLSSimilarityFactory
import edu.utdallas.hlt.medbase.umls.UMLSSimilarityFactory.Similarity
import edu.utdallas.hltri.Resource.using
import edu.utdallas.hltri.io.StusMagicLargeFileReader
import java.io.File
import java.net.InetAddress
import scala.collection.JavaConverters._

object MKG_UMLS_Analyzer {
  def main(args: Array[String]) {
    val graph = new File(args(0))
    val simHost = InetAddress.getByName(args(1))
    val simPort = args(2).toInt
    val simMeasure = Similarity.valueOf(args(3))

    val splitter = Splitter.on('\t')
    using(new UMLSSimilarityFactory(simHost, simPort).getUMLSSimilarity(simMeasure)){ metric =>
      using(new StusMagicLargeFileReader(graph)){ data =>
        for (line <- Iterator.continually(data.readLine).takeWhile(_ != null)) {
          val Seq(c1, a1, c2, a2, _*) = splitter.split(line).asScala.toSeq
          val sim = metric.apply(c1, c2)
          println(s"$c1/$a1 → $c2/$a2 = $sim")
        }
      }
    }
  }
}
