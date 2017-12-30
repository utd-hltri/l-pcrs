package edu.utdallas.hltri.util

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

/**
 * Created by travis on 2/10/14.
 */

trait IntervalView[T] {
  def asInterval(t: T): Interval1D
}

object IntervalTree  {
  def empty[A](implicit interval: IntervalView[A]) = new IntervalTree[A]

  def apply[A](keys: A*)(implicit interval: IntervalView[A]): IntervalTree[A] = {
    val t: IntervalTree[A] = empty
    for (key <- keys) { t += key }
    t
  }

  def newBuilder[T](implicit interval: IntervalView[T]): mutable.Builder[T, IntervalTree[T]] =
    new scala.collection.mutable.SetBuilder[T, IntervalTree[T]](empty)

  implicit def canBuildFrom[T](implicit interval: IntervalView[T]):
  CanBuildFrom[IntervalTree[_], T, IntervalTree[T]] =
    new CanBuildFrom[IntervalTree[_], T, IntervalTree[T]] {
      def apply(from: IntervalTree[_]): mutable.Builder[T, IntervalTree[T]] = newBuilder[T]
      def apply(): mutable.Builder[T, IntervalTree[T]] = newBuilder[T]
    }

}


class IntervalTree[T] (implicit val interval: IntervalView[T])
  extends mutable.Set[T] with mutable.SetLike[T, IntervalTree[T]] with Serializable {

  val tree = new IntervalST[T]()

  type Node = IntervalST[T]#Node

  /**
   * SetLike members
   */
  override def -=(elem: T): this.type = {
    tree.remove(interval.asInterval(elem))
    this
  }

  override def +=(elem: T): this.type = {
    tree.put(interval.asInterval(elem), elem)
    this
  }

  override def contains(elem: T): Boolean = tree.contains(interval.asInterval(elem))

  override def empty = new IntervalTree[T]

  override def iterator: Iterator[T] = {
    def visit(node: Node): Iterator[T] = {
      if (node == null) Iterator[T]()
      else visit(node.left) ++ Iterator[T](node.value) ++ visit(node.right)
    }

    visit(tree.root)
  }


}
