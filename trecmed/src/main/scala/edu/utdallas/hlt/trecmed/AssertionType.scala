package edu.utdallas.hlt.trecmed

/**
 * Date: 11/13/12
 * Time: 4:12 PM
 * @author travis
 * @VERSION 1.0
 */
object AssertionType extends Enumeration {
  val ABSENT = Value("ABSENT")
  val ASSOCIATED_WITH_SOMEONE_ELSE = Value("ASSOCIATED_WITH_SOMEONE_ELSE")
  val CONDITIONAL = Value("CONDITIONAL")
  val CONDUCTED = Value("CONDUCTED")
  val HISTORICAL = Value("HISTORICAL")
  val HYPOTHETICAL = Value("HYPOTHETICAL")
  val ONGOING = Value("ONGOING")
  val ORDERED = Value("ORDERED")
  val POSSIBLE = Value("POSSIBLE")
  val PRESCRIBED = Value("PRESCRIBED")
  val PRESENT = Value("PRESENT")
  val SUGGESTED = Value("SUGGESTED")
}
