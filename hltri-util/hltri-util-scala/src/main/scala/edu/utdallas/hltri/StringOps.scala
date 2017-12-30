package edu.utdallas.hltri

import edu.utdallas.hltri.util.Strings

object StringOps {
  implicit class EnhancedString(val str: String) extends AnyVal {
    def substringUntil(start: Int, char: Character): String =
      Strings.substringUntil(str, start, char)
    def substringUntil(char: Character): String =
      Strings.substringUntil(str, char)
  }
}
