//package edu.utdallas.hltri.scribe.text.relation;
//
//import edu.utdallas.hltri.scribe.text.Document;
//import edu.utdallas.hltri.scribe.text.annotation.Annotation;
//import edu.utdallas.hltri.scribe.text.relation.Relation;
//
//import java.util.AbstractCollection;
//import java.util.Collection;
//import java.util.Iterator;
//
//public class RelationImpl<S extends Annotation> extends AbstractCollection<S> implements Relation<S> {
//  protected final Document document;
//  protected final gate.relations.Relation relation;
//  protected final Collection<S> members;
//
//  protected RelationImpl(Document document, gate.relations.Relation gateRelation, Collection<S> members) {
//    this.document = document;
//    this.relation = gateRelation;
//    this.members = members;
//  }
//
//  public Collection<S> members() {
//    return members;
//  }
//
//  @Override public final String toString() {
//    return relation.getType();
//  }
//
//  @Override public Document getDocument() {
//    return document;
//  }
//
//  @Override public gate.relations.Relation asGate() {
//    return relation;
//  }
//
//  /**
//   * Returns an iterator over the elements contained in this collection.
//   *
//   * @return an iterator over the elements contained in this collection
//   */
//  @Override public Iterator<S> iterator() {
//    return members().iterator();
//  }
//
//  @Override public int size() {
//    return relation.getMembers().length;
//  }
//
//  /**
//   * Returns the length of this character sequence.  The length is the number
//   * of 16-bit <code>char</code>s in the sequence.
//   *
//   * @return the number of <code>char</code>s in this sequence
//   */
//  @Override public int length() {
//    return toString().length();
//  }
//
//  /**
//   * Returns the <code>char</code> value at the specified index.  An index ranges from zero
//   * to <tt>length() - 1</tt>.  The first <code>char</code> value of the sequence is at
//   * index zero, the next at index one, and so on, as for array
//   * indexing.
//   * <p>
//   * <p>If the <code>char</code> value specified by the index is a
//   * <a href="{@docRoot}/java/lang/Character.html#unicode">surrogate</a>, the surrogate
//   * value is returned.
//   *
//   * @param index the index of the <code>char</code> value to be returned
//   * @return the specified <code>char</code> value
//   * @throws IndexOutOfBoundsException if the <tt>index</tt> argument is negative or not less than
//   *                                   <tt>length()</tt>
//   */
//  @Override public char charAt(int index) {
//    return toString().charAt(index);
//  }
//
//  /**
//   * Returns a <code>CharSequence</code> that is a subsequence of this sequence.
//   * The subsequence starts with the <code>char</code> value at the specified index and
//   * ends with the <code>char</code> value at index <tt>end - 1</tt>.  The length
//   * (in <code>char</code>s) of the
//   * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt>
//   * then an empty sequence is returned.
//   *
//   * @param start the start index, inclusive
//   * @param end   the end index, exclusive
//   * @return the specified subsequence
//   * @throws IndexOutOfBoundsException if <tt>start</tt> or <tt>end</tt> are negative,
//   *                                   if <tt>end</tt> is greater than <tt>length()</tt>,
//   *                                   or if <tt>start</tt> is greater than <tt>end</tt>
//   */
//  @Override public CharSequence subSequence(int start, int end) {
//    return toString().subSequence(start, end);
//  }
//}
