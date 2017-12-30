package edu.utdallas.hltri.util

/**
 * Created by travis on 2/21/14.
 */
object ListOps {
  implicit class EnhancedList[Y](val list: Seq[Y]) extends AnyVal {

    def viewIndexedWindow(i: Int, n: Int): Iterator[(Y, Int)] = {
      val prefix = for (j <- (n to 1).iterator if i - j >= 0) yield (list(i - j), -j)
      val suffix = for (j <- (0 to n).iterator if i + j < list.length) yield (list(i + j), j)
      prefix ++ suffix
    }

  }
}
