package edu.utdallas.hlt.trecmed.math

import collection.GenIterable
import math.Numeric

/**
 * Date: 11/13/12
 * Time: 4:09 PM
 * @author travis
 * @VERSION 1.0
 */
object Implicits {
  implicit class NumericIterableExtensions[T : Numeric](self : GenIterable[T]) {
    val numeric = implicitly[Numeric[T]]

    import numeric._

    /*
     * Pythagorean means
     */
    def geometricMean(): Double = math.pow(self.reduce((x, y) => (x * y)).toDouble(), 1.0 / self.size)

    def harmonicMean(): Double = self.size / self.foldLeft(0.0)((a, x) => a + (1.0 / x.toDouble))

    def arithmeticMean(): Double = self.reduce((x, y) => x + y).toDouble / self.size

    /*
     * Root mean square
     */
    def quadraticMean(): Double = math.sqrt(self.reduce((x, y) => x * x + y * y).toDouble / self.size)
  }
}
