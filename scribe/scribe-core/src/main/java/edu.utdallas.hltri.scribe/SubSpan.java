package edu.utdallas.hltri.scribe;

import java.util.Arrays;


/**
 * Created by trg19 on 8/27/2016.
 */
public class SubSpan implements CharSequence, Comparable<CharSequence> {

  /** The value is used for character storage. */
  private final char value[];

  /** The offset is the first index of the storage that is used. */
  private final int offset;

  /** The count is the number of characters in the String. */
  private final int count;

  /** Cache the hash code for the string */
  private int hash; // Default to 0

  /**
   * Initializes a newly created {@code String} object so that it represents
   * the same sequence of characters as the argument; in other words, the
   * newly created string is a copy of the argument string. Unless an
   * explicit copy of {@code original} is needed, use of this constructor is
   * unnecessary since Strings are immutable.
   *
   * @param  original
   *         A {@code String}
   */
  public SubSpan(String original, int offset, int count) {
    this(original.toCharArray(), offset, count);
  }

  /**
   * Allocates a new {@code String} that contains characters from a subarray
   * of the character array argument. The {@code offset} argument is the
   * index of the first character of the subarray and the {@code count}
   * argument specifies the length of the subarray. The contents of the
   * subarray are copied; subsequent modification of the character array does
   * not affect the newly created string.
   *
   * @param  value
   *         Array that is the source of characters
   *
   * @param  offset
   *         The initial offset
   *
   * @param  count
   *         The length
   *
   * @throws  IndexOutOfBoundsException
   *          If the {@code offset} and {@code count} arguments index
   *          characters outside the bounds of the {@code value} array
   */
  public SubSpan(char value[], int offset, int count) {
    if (offset < 0) {
      throw new StringIndexOutOfBoundsException(offset);
    }
    if (count < 0) {
      throw new StringIndexOutOfBoundsException(count);
    }
    // Note: offset or count might be near -1>>>1.
    if (offset > value.length - count) {
      throw new StringIndexOutOfBoundsException(offset + count);
    }
    this.offset = 0;
    this.count = count;
    this.value = Arrays.copyOfRange(value, offset, offset+count);
  }


  // Package private constructor which shares value array for speed.
  SubSpan(int offset, int count, char value[]) {
    this.value = value;
    this.offset = offset;
    this.count = count;
  }

  /**
   * Returns the length of this string.
   * The length is equal to the number of <a href="Character.html#unicode">Unicode
   * code units</a> in the string.
   *
   * @return  the length of the sequence of characters represented by this
   *          object.
   */
  public int length() {
    return count;
  }

  /**
   * Returns <tt>true</tt> if, and only if, {@link #length()} is <tt>0</tt>.
   *
   * @return <tt>true</tt> if {@link #length()} is <tt>0</tt>, otherwise
   * <tt>false</tt>
   *
   * @since 1.6
   */
  public boolean isEmpty() {
    return count == 0;
  }

  /**
   * Returns the <code>char</code> value at the
   * specified index. An index ranges from <code>0</code> to
   * <code>length() - 1</code>. The first <code>char</code> value of the sequence
   * is at index <code>0</code>, the next at index <code>1</code>,
   * and so on, as for array indexing.
   *
   * <p>If the <code>char</code> value specified by the index is a
   * <a href="Character.html#unicode">surrogate</a>, the surrogate
   * value is returned.
   *
   * @param      index   the index of the <code>char</code> value.
   * @return     the <code>char</code> value at the specified index of this string.
   *             The first <code>char</code> value is at index <code>0</code>.
   * @exception  IndexOutOfBoundsException  if the <code>index</code>
   *             argument is negative or not less than the length of this
   *             string.
   */
  public char charAt(int index) {
    if ((index < 0) || (index >= count)) {
      throw new StringIndexOutOfBoundsException(index);
    }
    return value[index + offset];
  }


  /**
   * Copy characters from this string into dst starting at dstBegin.
   * This method doesn't perform any range checking.
   */
  void getChars(char dst[], int dstBegin) {
    System.arraycopy(value, offset, dst, dstBegin, count);
  }

  /**
   * Copies characters from this string into the destination character
   * array.
   * <p>
   * The first character to be copied is at index <code>srcBegin</code>;
   * the last character to be copied is at index <code>srcEnd-1</code>
   * (thus the total number of characters to be copied is
   * <code>srcEnd-srcBegin</code>). The characters are copied into the
   * subarray of <code>dst</code> starting at index <code>dstBegin</code>
   * and ending at index:
   * <p><blockquote><pre>
   *     dstbegin + (srcEnd-srcBegin) - 1
   * </pre></blockquote>
   *
   * @param      srcBegin   index of the first character in the string
   *                        to copy.
   * @param      srcEnd     index after the last character in the string
   *                        to copy.
   * @param      dst        the destination array.
   * @param      dstBegin   the start offset in the destination array.
   * @exception IndexOutOfBoundsException If any of the following
   *            is true:
   *            <ul><li><code>srcBegin</code> is negative.
   *            <li><code>srcBegin</code> is greater than <code>srcEnd</code>
   *            <li><code>srcEnd</code> is greater than the length of this
   *                string
   *            <li><code>dstBegin</code> is negative
   *            <li><code>dstBegin+(srcEnd-srcBegin)</code> is larger than
   *                <code>dst.length</code></ul>
   */
  public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
    if (srcBegin < 0) {
      throw new StringIndexOutOfBoundsException(srcBegin);
    }
    if (srcEnd > count) {
      throw new StringIndexOutOfBoundsException(srcEnd);
    }
    if (srcBegin > srcEnd) {
      throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
    }
    System.arraycopy(value, offset + srcBegin, dst, dstBegin,
        srcEnd - srcBegin);
  }


  /**
   * Compares this string to the specified object.  The result is {@code
   * true} if and only if the argument is not {@code null} and is a {@code
   * String} object that represents the same sequence of characters as this
   * object.
   *
   * @param  anObject
   *         The object to compare this {@code String} against
   *
   * @return  {@code true} if the given object represents a {@code String}
   *          equivalent to this string, {@code false} otherwise
   *
   */
  public boolean equals(Object anObject) {
    if (this == anObject) {
      return true;
    }
    if (anObject instanceof SubSpan) {
      SubSpan anotherString = (SubSpan)anObject;
      int n = count;
      if (n == anotherString.count) {
        char v1[] = value;
        char v2[] = anotherString.value;
        int i = offset;
        int j = anotherString.offset;
        while (n-- != 0) {
          if (v1[i++] != v2[j++])
            return false;
        }
        return true;
      }
    }
    return false;
  }


  /**
   * Compares this string to the specified {@code CharSequence}.  The result
   * is {@code true} if and only if this {@code String} represents the same
   * sequence of char values as the specified sequence.
   *
   * @param  cs
   *         The sequence to compare this {@code String} against
   *
   * @return  {@code true} if this {@code String} represents the same
   *          sequence of char values as the specified sequence, {@code
   *          false} otherwise
   *
   * @since  1.5
   */
  public boolean contentEquals(CharSequence cs) {
    if (count != cs.length())
      return false;
    int i = offset;
    int j = 0;
    int n = count;
    while (n-- != 0) {
      if (value[i++] != cs.charAt(j++))
        return false;
    }
    return true;
  }

  /**
   * Compares two strings lexicographically.
   * The comparison is based on the Unicode value of each character in
   * the strings. The character sequence represented by this
   * <code>String</code> object is compared lexicographically to the
   * character sequence represented by the argument string. The result is
   * a negative integer if this <code>String</code> object
   * lexicographically precedes the argument string. The result is a
   * positive integer if this <code>String</code> object lexicographically
   * follows the argument string. The result is zero if the strings
   * are equal; <code>compareTo</code> returns <code>0</code> exactly when
   * the {@link #equals(Object)} method would return <code>true</code>.
   * <p>
   * This is the definition of lexicographic ordering. If two strings are
   * different, then either they have different characters at some index
   * that is a valid index for both strings, or their lengths are different,
   * or both. If they have different characters at one or more index
   * positions, let <i>k</i> be the smallest such index; then the string
   * whose character at position <i>k</i> has the smaller value, as
   * determined by using the &lt; operator, lexicographically precedes the
   * other string. In this case, <code>compareTo</code> returns the
   * difference of the two character values at position <code>k</code> in
   * the two string -- that is, the value:
   * <blockquote><pre>
   * this.charAt(k)-anotherString.charAt(k)
   * </pre></blockquote>
   * If there is no index position at which they differ, then the shorter
   * string lexicographically precedes the longer string. In this case,
   * <code>compareTo</code> returns the difference of the lengths of the
   * strings -- that is, the value:
   * <blockquote><pre>
   * this.length()-anotherString.length()
   * </pre></blockquote>
   *
   * @param   anotherString   the <code>String</code> to be compared.
   * @return  the value <code>0</code> if the argument string is equal to
   *          this string; a value less than <code>0</code> if this string
   *          is lexicographically less than the string argument; and a
   *          value greater than <code>0</code> if this string is
   *          lexicographically greater than the string argument.
   */
  public int compareTo(CharSequence anotherString) {
    int len1 = count;
    int len2 = anotherString.length();
    int n = Math.min(len1, len2);
    char v1[] = value;
    char v2[] = anotherString.toString().toCharArray();
    int i = offset;
    int j = 0;

    if (i == j) {
      int k = i;
      int lim = n + i;
      while (k < lim) {
        char c1 = v1[k];
        char c2 = v2[k];
        if (c1 != c2) {
          return c1 - c2;
        }
        k++;
      }
    } else {
      while (n-- != 0) {
        char c1 = v1[i++];
        char c2 = v2[j++];
        if (c1 != c2) {
          return c1 - c2;
        }
      }
    }
    return len1 - len2;
  }

  /**
   * Tests if two string regions are equal.
   * <p>
   * A substring of this <tt>String</tt> object is compared to a substring
   * of the argument other. The result is true if these substrings
   * represent identical character sequences. The substring of this
   * <tt>String</tt> object to be compared begins at index <tt>toffset</tt>
   * and has length <tt>len</tt>. The substring of other to be compared
   * begins at index <tt>ooffset</tt> and has length <tt>len</tt>. The
   * result is <tt>false</tt> if and only if at least one of the following
   * is true:
   * <ul><li><tt>toffset</tt> is negative.
   * <li><tt>ooffset</tt> is negative.
   * <li><tt>toffset+len</tt> is greater than the length of this
   * <tt>String</tt> object.
   * <li><tt>ooffset+len</tt> is greater than the length of the other
   * argument.
   * <li>There is some nonnegative integer <i>k</i> less than <tt>len</tt>
   * such that:
   * <tt>this.charAt(toffset+<i>k</i>)&nbsp;!=&nbsp;other.charAt(ooffset+<i>k</i>)</tt>
   * </ul>
   *
   * @param   toffset   the starting offset of the subregion in this string.
   * @param   other     the string argument.
   * @param   ooffset   the starting offset of the subregion in the string
   *                    argument.
   * @param   len       the number of characters to compare.
   * @return  <code>true</code> if the specified subregion of this string
   *          exactly matches the specified subregion of the string argument;
   *          <code>false</code> otherwise.
   */
  public boolean regionMatches(int toffset, CharSequence other, int ooffset,
                               int len) {
    char ta[] = value;
    int to = offset + toffset;
    char pa[] = other.toString().toCharArray();
    int po = ooffset;
    // Note: toffset, ooffset, or len might be near -1>>>1.
    if ((ooffset < 0) || (toffset < 0) || (toffset > (long)count - len)
        || (ooffset > (long)other.length() - len)) {
      return false;
    }
    while (len-- > 0) {
      if (ta[to++] != pa[po++]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests if two string regions are equal.
   * <p>
   * A substring of this <tt>String</tt> object is compared to a substring
   * of the argument <tt>other</tt>. The result is <tt>true</tt> if these
   * substrings represent character sequences that are the same, ignoring
   * case if and only if <tt>ignoreCase</tt> is true. The substring of
   * this <tt>String</tt> object to be compared begins at index
   * <tt>toffset</tt> and has length <tt>len</tt>. The substring of
   * <tt>other</tt> to be compared begins at index <tt>ooffset</tt> and
   * has length <tt>len</tt>. The result is <tt>false</tt> if and only if
   * at least one of the following is true:
   * <ul><li><tt>toffset</tt> is negative.
   * <li><tt>ooffset</tt> is negative.
   * <li><tt>toffset+len</tt> is greater than the length of this
   * <tt>String</tt> object.
   * <li><tt>ooffset+len</tt> is greater than the length of the other
   * argument.
   * <li><tt>ignoreCase</tt> is <tt>false</tt> and there is some nonnegative
   * integer <i>k</i> less than <tt>len</tt> such that:
   * <blockquote><pre>
   * this.charAt(toffset+k) != other.charAt(ooffset+k)
   * </pre></blockquote>
   * <li><tt>ignoreCase</tt> is <tt>true</tt> and there is some nonnegative
   * integer <i>k</i> less than <tt>len</tt> such that:
   * <blockquote><pre>
   * Character.toLowerCase(this.charAt(toffset+k)) !=
   Character.toLowerCase(other.charAt(ooffset+k))
   * </pre></blockquote>
   * and:
   * <blockquote><pre>
   * Character.toUpperCase(this.charAt(toffset+k)) !=
   *         Character.toUpperCase(other.charAt(ooffset+k))
   * </pre></blockquote>
   * </ul>
   *
   * @param   ignoreCase   if <code>true</code>, ignore case when comparing
   *                       characters.
   * @param   toffset      the starting offset of the subregion in this
   *                       string.
   * @param   other        the string argument.
   * @param   ooffset      the starting offset of the subregion in the string
   *                       argument.
   * @param   len          the number of characters to compare.
   * @return  <code>true</code> if the specified subregion of this string
   *          matches the specified subregion of the string argument;
   *          <code>false</code> otherwise. Whether the matching is exact
   *          or case insensitive depends on the <code>ignoreCase</code>
   *          argument.
   */
  public boolean regionMatches(boolean ignoreCase, int toffset,
                               CharSequence other, int ooffset, int len) {
    char ta[] = value;
    int to = offset + toffset;
    char pa[] = other.toString().toCharArray();
    int po = ooffset;
    // Note: toffset, ooffset, or len might be near -1>>>1.
    if ((ooffset < 0) || (toffset < 0) || (toffset > (long)count - len) ||
        (ooffset > (long)other.length() - len)) {
      return false;
    }
    while (len-- > 0) {
      char c1 = ta[to++];
      char c2 = pa[po++];
      if (c1 == c2) {
        continue;
      }
      if (ignoreCase) {
        // If characters don't match but case may be ignored,
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should
        // continue.
        char u1 = Character.toUpperCase(c1);
        char u2 = Character.toUpperCase(c2);
        if (u1 == u2) {
          continue;
        }
        // Unfortunately, conversion to uppercase does not work properly
        // for the Georgian alphabet, which has strange rules about case
        // conversion.  So we need to make one last check before
        // exiting.
        if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
          continue;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Tests if the substring of this string beginning at the
   * specified index starts with the specified prefix.
   *
   * @param   prefix    the prefix.
   * @param   toffset   where to begin looking in this string.
   * @return  <code>true</code> if the character sequence represented by the
   *          argument is a prefix of the substring of this object starting
   *          at index <code>toffset</code>; <code>false</code> otherwise.
   *          The result is <code>false</code> if <code>toffset</code> is
   *          negative or greater than the length of this
   *          <code>String</code> object; otherwise the result is the same
   *          as the result of the expression
   *          <pre>
   *          this.substring(toffset).startsWith(prefix)
   *          </pre>
   */
  public boolean startsWith(CharSequence prefix, int toffset) {
    int to = offset + toffset;
    char pa[] = prefix.toString().toCharArray();
    int po = 0;
    int pc = prefix.length();
    // Note: toffset might be near -1>>>1.
    if ((toffset < 0) || (toffset > count - pc)) {
      return false;
    }
    while (--pc >= 0) {
      if (value[to++] != pa[po++]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests if this string starts with the specified prefix.
   *
   * @param   prefix   the prefix.
   * @return  <code>true</code> if the character sequence represented by the
   *          argument is a prefix of the character sequence represented by
   *          this string; <code>false</code> otherwise.
   *          Note also that <code>true</code> will be returned if the
   *          argument is an empty string or is equal to this
   *          <code>String</code> object as determined by the
   *          {@link #equals(Object)} method.
   * @since   1. 0
   */
  public boolean startsWith(CharSequence prefix) {
    return startsWith(prefix, 0);
  }

  /**
   * Tests if this string ends with the specified suffix.
   *
   * @param   suffix   the suffix.
   * @return  <code>true</code> if the character sequence represented by the
   *          argument is a suffix of the character sequence represented by
   *          this object; <code>false</code> otherwise. Note that the
   *          result will be <code>true</code> if the argument is the
   *          empty string or is equal to this <code>String</code> object
   *          as determined by the {@link #equals(Object)} method.
   */
  public boolean endsWith(CharSequence suffix) {
    return startsWith(suffix, count - suffix.length());
  }

  /**
   * Returns a hash code for this string. The hash code for a
   * <code>String</code> object is computed as
   * <blockquote><pre>
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   * using <code>int</code> arithmetic, where <code>s[i]</code> is the
   * <i>i</i>th character of the string, <code>n</code> is the length of
   * the string, and <code>^</code> indicates exponentiation.
   * (The hash value of the empty string is zero.)
   *
   * @return  a hash code value for this object.
   */
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      int off = offset;
      char val[] = value;
      int len = count;

      for (int i = 0; i < len; i++) {
        h = 31*h + val[off++];
      }
      hash = h;
    }
    return h;
  }

  /**
   * Returns the index within this string of the first occurrence of
   * the specified character. If a character with value
   * <code>ch</code> occurs in the character sequence represented by
   * this <code>String</code> object, then the index (in Unicode
   * code units) of the first such occurrence is returned. For
   * values of <code>ch</code> in the range from 0 to 0xFFFF
   * (inclusive), this is the smallest value <i>k</i> such that:
   * <blockquote><pre>
   * this.charAt(<i>k</i>) == ch
   * </pre></blockquote>
   * is true. For other values of <code>ch</code>, it is the
   * smallest value <i>k</i> such that:
   * <blockquote><pre>
   * this.codePointAt(<i>k</i>) == ch
   * </pre></blockquote>
   * is true. In either case, if no such character occurs in this
   * string, then <code>-1</code> is returned.
   *
   * @param   ch   a character (Unicode code point).
   * @return  the index of the first occurrence of the character in the
   *          character sequence represented by this object, or
   *          <code>-1</code> if the character does not occur.
   */
  public int indexOf(int ch) {
    return indexOf(ch, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified character, starting the search at the specified index.
   * <p>
   * If a character with value <code>ch</code> occurs in the
   * character sequence represented by this <code>String</code>
   * object at an index no smaller than <code>fromIndex</code>, then
   * the index of the first such occurrence is returned. For values
   * of <code>ch</code> in the range from 0 to 0xFFFF (inclusive),
   * this is the smallest value <i>k</i> such that:
   * <blockquote><pre>
   * (this.charAt(<i>k</i>) == ch) && (<i>k</i> &gt;= fromIndex)
   * </pre></blockquote>
   * is true. For other values of <code>ch</code>, it is the
   * smallest value <i>k</i> such that:
   * <blockquote><pre>
   * (this.codePointAt(<i>k</i>) == ch) && (<i>k</i> &gt;= fromIndex)
   * </pre></blockquote>
   * is true. In either case, if no such character occurs in this
   * string at or after position <code>fromIndex</code>, then
   * <code>-1</code> is returned.
   *
   * <p>
   * There is no restriction on the value of <code>fromIndex</code>. If it
   * is negative, it has the same effect as if it were zero: this entire
   * string may be searched. If it is greater than the length of this
   * string, it has the same effect as if it were equal to the length of
   * this string: <code>-1</code> is returned.
   *
   * <p>All indices are specified in <code>char</code> values
   * (Unicode code units).
   *
   * @param   ch          a character (Unicode code point).
   * @param   fromIndex   the index to start the search from.
   * @return  the index of the first occurrence of the character in the
   *          character sequence represented by this object that is greater
   *          than or equal to <code>fromIndex</code>, or <code>-1</code>
   *          if the character does not occur.
   */
  public int indexOf(int ch, int fromIndex) {
    int max = offset + count;
    char v[] = value;

    if (fromIndex < 0) {
      fromIndex = 0;
    } else if (fromIndex >= count) {
      // Note: fromIndex might be near -1>>>1.
      return -1;
    }

    int i = offset + fromIndex;
    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
      // handle most cases here (ch is a BMP code point or a
      // negative value (invalid code point))
      for (; i < max ; i++) {
        if (v[i] == ch) {
          return i - offset;
        }
      }
      return -1;
    }

    if (ch <= Character.MAX_CODE_POINT) {
      // handle supplementary characters here
      char[] surrogates = Character.toChars(ch);
      for (; i < max; i++) {
        if (v[i] == surrogates[0]) {
          if (i + 1 == max) {
            break;
          }
          if (v[i+1] == surrogates[1]) {
            return i - offset;
          }
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index within this string of the last occurrence of
   * the specified character. For values of <code>ch</code> in the
   * range from 0 to 0xFFFF (inclusive), the index (in Unicode code
   * units) returned is the largest value <i>k</i> such that:
   * <blockquote><pre>
   * this.charAt(<i>k</i>) == ch
   * </pre></blockquote>
   * is true. For other values of <code>ch</code>, it is the
   * largest value <i>k</i> such that:
   * <blockquote><pre>
   * this.codePointAt(<i>k</i>) == ch
   * </pre></blockquote>
   * is true.  In either case, if no such character occurs in this
   * string, then <code>-1</code> is returned.  The
   * <code>String</code> is searched backwards starting at the last
   * character.
   *
   * @param   ch   a character (Unicode code point).
   * @return  the index of the last occurrence of the character in the
   *          character sequence represented by this object, or
   *          <code>-1</code> if the character does not occur.
   */
  public int lastIndexOf(int ch) {
    return lastIndexOf(ch, count - 1);
  }

  /**
   * Returns the index within this string of the last occurrence of
   * the specified character, searching backward starting at the
   * specified index. For values of <code>ch</code> in the range
   * from 0 to 0xFFFF (inclusive), the index returned is the largest
   * value <i>k</i> such that:
   * <blockquote><pre>
   * (this.charAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)
   * </pre></blockquote>
   * is true. For other values of <code>ch</code>, it is the
   * largest value <i>k</i> such that:
   * <blockquote><pre>
   * (this.codePointAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)
   * </pre></blockquote>
   * is true. In either case, if no such character occurs in this
   * string at or before position <code>fromIndex</code>, then
   * <code>-1</code> is returned.
   *
   * <p>All indices are specified in <code>char</code> values
   * (Unicode code units).
   *
   * @param   ch          a character (Unicode code point).
   * @param   fromIndex   the index to start the search from. There is no
   *          restriction on the value of <code>fromIndex</code>. If it is
   *          greater than or equal to the length of this string, it has
   *          the same effect as if it were equal to one less than the
   *          length of this string: this entire string may be searched.
   *          If it is negative, it has the same effect as if it were -1:
   *          -1 is returned.
   * @return  the index of the last occurrence of the character in the
   *          character sequence represented by this object that is less
   *          than or equal to <code>fromIndex</code>, or <code>-1</code>
   *          if the character does not occur before that point.
   */
  public int lastIndexOf(int ch, int fromIndex) {
    int min = offset;
    char v[] = value;

    int i = offset + ((fromIndex >= count) ? count - 1 : fromIndex);

    if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
      // handle most cases here (ch is a BMP code point or a
      // negative value (invalid code point))
      for (; i >= min ; i--) {
        if (v[i] == ch) {
          return i - offset;
        }
      }
      return -1;
    }

    int max = offset + count;
    if (ch <= Character.MAX_CODE_POINT) {
      // handle supplementary characters here
      char[] surrogates = Character.toChars(ch);
      for (; i >= min; i--) {
        if (v[i] == surrogates[0]) {
          if (i + 1 == max) {
            break;
          }
          if (v[i+1] == surrogates[1]) {
            return i - offset;
          }
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified substring. The integer returned is the smallest value
   * <i>k</i> such that:
   * <blockquote><pre>
   * this.startsWith(str, <i>k</i>)
   * </pre></blockquote>
   * is <code>true</code>.
   *
   * @param   str   any string.
   * @return  if the string argument occurs as a substring within this
   *          object, then the index of the first character of the first
   *          such substring is returned; if it does not occur as a
   *          substring, <code>-1</code> is returned.
   */
  public int indexOf(CharSequence str) {
    return indexOf(str, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified substring, starting at the specified index.  The integer
   * returned is the smallest value <tt>k</tt> for which:
   * <blockquote><pre>
   *     k &gt;= Math.min(fromIndex, this.length()) && this.startsWith(str, k)
   * </pre></blockquote>
   * If no such value of <i>k</i> exists, then -1 is returned.
   *
   * @param   str         the substring for which to search.
   * @param   fromIndex   the index from which to start the search.
   * @return  the index within this string of the first occurrence of the
   *          specified substring, starting at the specified index.
   */
  public int indexOf(CharSequence str, int fromIndex) {
    return indexOf(value, offset, count,
        str.toString().toCharArray(), 0, str.length(), fromIndex);
  }

  /**
   * Code shared by String and StringBuffer to do searches. The
   * source is the character array being searched, and the target
   * is the string being searched for.
   *
   * @param   source       the characters being searched.
   * @param   sourceOffset offset of the source string.
   * @param   sourceCount  count of the source string.
   * @param   target       the characters being searched for.
   * @param   targetOffset offset of the target string.
   * @param   targetCount  count of the target string.
   * @param   fromIndex    the index to begin searching from.
   */
  private static int indexOf(char[] source, int sourceOffset, int sourceCount,
                     char[] target, int targetOffset, int targetCount,
                     int fromIndex) {
    if (fromIndex >= sourceCount) {
      return (targetCount == 0 ? sourceCount : -1);
    }
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    if (targetCount == 0) {
      return fromIndex;
    }

    char first  = target[targetOffset];
    int max = sourceOffset + (sourceCount - targetCount);

    for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
      if (source[i] != first) {
        while (++i <= max && source[i] != first);
      }

            /* Found first character, now look at the rest of v2 */
      if (i <= max) {
        int j = i + 1;
        int end = j + targetCount - 1;
        for (int k = targetOffset + 1; j < end && source[j] ==
            target[k]; j++, k++);

        if (j == end) {
                    /* Found whole string. */
          return i - sourceOffset;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index within this string of the rightmost occurrence
   * of the specified substring.  The rightmost empty string "" is
   * considered to occur at the index value <code>this.length()</code>.
   * The returned index is the largest value <i>k</i> such that
   * <blockquote><pre>
   * this.startsWith(str, k)
   * </pre></blockquote>
   * is true.
   *
   * @param   str   the substring to search for.
   * @return  if the string argument occurs one or more times as a substring
   *          within this object, then the index of the first character of
   *          the last such substring is returned. If it does not occur as
   *          a substring, <code>-1</code> is returned.
   */
  public int lastIndexOf(CharSequence str) {
    return lastIndexOf(str, count);
  }

  /**
   * Returns the index within this string of the last occurrence of the
   * specified substring, searching backward starting at the specified index.
   * The integer returned is the largest value <i>k</i> such that:
   * <blockquote><pre>
   *     k &lt;= Math.min(fromIndex, this.length()) && this.startsWith(str, k)
   * </pre></blockquote>
   * If no such value of <i>k</i> exists, then -1 is returned.
   *
   * @param   str         the substring to search for.
   * @param   fromIndex   the index to start the search from.
   * @return  the index within this string of the last occurrence of the
   *          specified substring.
   */
  public int lastIndexOf(CharSequence str, int fromIndex) {
    return lastIndexOf(value, offset, count,
        str.toString().toCharArray(), 0, str.length(), fromIndex);
  }

  /**
   * Code shared by String and StringBuffer to do searches. The
   * source is the character array being searched, and the target
   * is the string being searched for.
   *
   * @param   source       the characters being searched.
   * @param   sourceOffset offset of the source string.
   * @param   sourceCount  count of the source string.
   * @param   target       the characters being searched for.
   * @param   targetOffset offset of the target string.
   * @param   targetCount  count of the target string.
   * @param   fromIndex    the index to begin searching from.
   */
  private static int lastIndexOf(char[] source, int sourceOffset, int sourceCount,
                                 char[] target, int targetOffset, int targetCount,
                                 int fromIndex) {
        /*
         * Check arguments; return immediately where possible. For
         * consistency, don't check for null str.
         */
    int rightIndex = sourceCount - targetCount;
    if (fromIndex < 0) {
      return -1;
    }
    if (fromIndex > rightIndex) {
      fromIndex = rightIndex;
    }
        /* Empty string always matches. */
    if (targetCount == 0) {
      return fromIndex;
    }

    int strLastIndex = targetOffset + targetCount - 1;
    char strLastChar = target[strLastIndex];
    int min = sourceOffset + targetCount - 1;
    int i = min + fromIndex;

    startSearchForLastChar:
    while (true) {
      while (i >= min && source[i] != strLastChar) {
        i--;
      }
      if (i < min) {
        return -1;
      }
      int j = i - 1;
      int start = j - (targetCount - 1);
      int k = strLastIndex - 1;

      while (j > start) {
        if (source[j--] != target[k--]) {
          i--;
          continue startSearchForLastChar;
        }
      }
      return start - sourceOffset + 1;
    }
  }

  /**
   * Returns a new string that is a substring of this string. The
   * substring begins with the character at the specified index and
   * extends to the end of this string. <p>
   * Examples:
   * <blockquote><pre>
   * "unhappy".substring(2) returns "happy"
   * "Harbison".substring(3) returns "bison"
   * "emptiness".substring(9) returns "" (an empty string)
   * </pre></blockquote>
   *
   * @param      beginIndex   the beginning index, inclusive.
   * @return     the specified substring.
   * @exception  IndexOutOfBoundsException  if
   *             <code>beginIndex</code> is negative or larger than the
   *             length of this <code>String</code> object.
   */
  public String substring(int beginIndex) {
    return substring(beginIndex, count);
  }

  /**
   * Returns a new string that is a substring of this string. The
   * substring begins at the specified <code>beginIndex</code> and
   * extends to the character at index <code>endIndex - 1</code>.
   * Thus the length of the substring is <code>endIndex-beginIndex</code>.
   * <p>
   * Examples:
   * <blockquote><pre>
   * "hamburger".substring(4, 8) returns "urge"
   * "smiles".substring(1, 5) returns "mile"
   * </pre></blockquote>
   *
   * @param      beginIndex   the beginning index, inclusive.
   * @param      endIndex     the ending index, exclusive.
   * @return     the specified substring.
   * @exception  IndexOutOfBoundsException  if the
   *             <code>beginIndex</code> is negative, or
   *             <code>endIndex</code> is larger than the length of
   *             this <code>String</code> object, or
   *             <code>beginIndex</code> is larger than
   *             <code>endIndex</code>.
   */
  public String substring(int beginIndex, int endIndex) {
    if (beginIndex < 0) {
      throw new StringIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > count) {
      throw new StringIndexOutOfBoundsException(endIndex);
    }
    if (beginIndex > endIndex) {
      throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
    }
    final int length = endIndex - beginIndex;
    final char[] values = new char [ length ];
    System.arraycopy(value, offset + beginIndex, values, 0, length);


    return ((beginIndex == 0) && (endIndex == count)) ? toString() :
        new String(values);
  }

  /**
   * Returns a new character sequence that is a subsequence of this sequence.
   *
   * <p> An invocation of this method of the form
   *
   * <blockquote><pre>
   * str.subSequence(begin,&nbsp;end)</pre></blockquote>
   *
   * behaves in exactly the same way as the invocation
   *
   * <blockquote><pre>
   * str.substring(begin,&nbsp;end)</pre></blockquote>
   *
   * This method is defined so that the <tt>String</tt> class can implement
   * the {@link CharSequence} interface. </p>
   *
   * @param      beginIndex   the begin index, inclusive.
   * @param      endIndex     the end index, exclusive.
   * @return     the specified subsequence.
   *
   * @throws  IndexOutOfBoundsException
   *          if <tt>beginIndex</tt> or <tt>endIndex</tt> are negative,
   *          if <tt>endIndex</tt> is greater than <tt>length()</tt>,
   *          or if <tt>beginIndex</tt> is greater than <tt>startIndex</tt>
   *
   * @since 1.4
   * @spec JSR-51
   */
  public CharSequence subSequence(int beginIndex, int endIndex) {
    return this.substring(beginIndex, endIndex);
  }

  /**
   * Returns true if and only if this string contains the specified
   * sequence of char values.
   *
   * @param s the sequence to search for
   * @return true if this string contains <code>s</code>, false otherwise
   * @throws NullPointerException if <code>s</code> is <code>null</code>
   * @since 1.5
   */
  public boolean contains(CharSequence s) {
    return indexOf(s.toString()) > -1;
  }

  /**
   * This object (which is already a string!) is itself returned.
   *
   * @return  the string itself.
   */
  public String toString() {
    return new String(toCharArray());
  }

  /**
   * Converts this string to a new character array.
   *
   * @return  a newly allocated character array whose length is the length
   *          of this string and whose contents are initialized to contain
   *          the character sequence represented by this string.
   */
  public char[] toCharArray() {
    char result[] = new char[count];
    getChars(0, count, result, 0);
    return result;
  }
}