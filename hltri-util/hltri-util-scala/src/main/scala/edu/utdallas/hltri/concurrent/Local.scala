package edu.utdallas.hltri.concurrent

import java.util.function.Supplier

/**
 * Created by travis on 7/21/14.
 */

object Local {
  @Deprecated
  def local[T](t: => T): ThreadLocal[T] = new ThreadLocal[T] {
    override def initialValue(): T = t
  }

  def apply[T](t: => T): ThreadLocal[T] = new ThreadLocal[T] {
    override def initialValue(): T = t
  }

  object Implicits {

    implicit class ThreadLocalWrapper[T](t: T) {
      def local: ThreadLocal[T] = ThreadLocal.withInitial(new Supplier[T] {
        override def get(): T = t
      })
    }

    implicit def unwrapLocal[T](tl: ThreadLocal[T]): T = tl.get
  }


}
