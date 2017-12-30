package edu.utdallas.hltri.io

import java.io.{BufferedReader, File, FileFilter}
import java.{lang, util}

import edu.utdallas.hltri.io.LazyIOUtils.LineIterator
import edu.utdallas.hltri.logging.Logging

import scala.collection.JavaConverters._

/**
 * Created by travis on 7/18/14.
 */
class LazyFileUtils extends LazyIOUtils with Logging {
  override def readBatchLines(reader: BufferedReader, batch: Int): lang.Iterable[String] =
    new lang.Iterable[String] {
      override def iterator(): util.Iterator[String] = {
        val it = for {block <- new LineIterator(reader).asScala.grouped(batch)
                      line <- block.par} yield line
        it.asJava
      }
    }

  private def buildIterator(file: File, filter: FileFilter): Iterator[File] = {
    if (file.isDirectory) {
      log.trace(s"Iterating inside directory $file")
      file.listFiles(filter).iterator.flatMap(child => buildIterator(child, filter))
    }
    else if (filter.accept(file)) {
      Iterator.single(file)
    }
    else {
      log.trace(s"Ignoring file $file: didn't match the filter")
      Iterator.empty
    }
  }

  override def iterateWithFilter(file: File, filter: FileFilter): lang.Iterable[File] = {
    new lang.Iterable[File]() {
      override def iterator(): util.Iterator[File] = buildIterator(file, filter).asJava
    }
  }
}
