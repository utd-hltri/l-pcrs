package edu.utdallas.hltri

import scala.language.reflectiveCalls

object Resource {

  // wrap a function so that we close the given resource
  def using[A, B <: { def close() }] (resource: B) (f: B => A): A = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }
}
