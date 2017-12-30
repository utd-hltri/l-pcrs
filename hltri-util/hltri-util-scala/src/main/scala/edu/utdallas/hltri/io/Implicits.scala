package edu.utdallas.hltri.io

import java.io.File
import java.nio.charset.Charset

import com.google.common.io.Files

import scala.io.Source


object Implicits {
  /** A wrapper around file, allowing iteration either on direct children
       or on directory tree */
  class ChildIterable(val file: File) extends Iterable[File] {
    def iterator: Iterator[File] = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
  }

  implicit val defaultCharset: Charset = Charset.defaultCharset()

  implicit class RichFile(val file: File) extends AnyVal {

    def children: Iterable[File] = new ChildIterable(file)
    def tree: Iterable[File] = Seq(file) ++ children.flatMap(child => new RichFile(child).tree)

    def parLines(blocks: Int = 256): Iterator[String] =
      for (block <- Source.fromFile(file).getLines().grouped(blocks);
                                           line <- block.par) yield line

    def seqLines(): Iterator[String] = Source.fromFile(file).getLines()

    def deleteAll(): Boolean = {
      var success = true
      if (file.isDirectory) {
        for (child <- file.listFiles()) {
          success = success && child.deleteAll()
        }
      }
      success && file.delete()
    }

    def read(): String = Files.toString(file, defaultCharset)
  }
}
