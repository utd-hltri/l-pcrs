package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.inquire.engines.SearchResultsList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

/**
 * Created by rmm120030 on 5/17/17 and made dank by Travis on 08/04/17
 */
public class SolrResultList extends SearchResultsList<SolrDocument, SolrResult> implements List<SolrResult> {
  private final QueryResponse response;

  SolrResultList(List<SolrResult> list, QueryResponse response) {
    super(response.getResults().getMaxScore(), (int) response.getResults().getNumFound(), list);
    this.response = response;
  }

  public QueryResponse getResponse() {
    return response;
  }

  @Override
  public int size() {
    return resultsList.size();
  }

  @Override
  public boolean isEmpty() {
    return resultsList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return resultsList.contains(o);
  }

  @Override
  public @Nonnull Iterator<SolrResult> iterator() {
    return resultsList.iterator();
  }

  @Override
  public @Nonnull Object[] toArray() {
    return resultsList.toArray();
  }

  @Override
  public @Nonnull <T> T[] toArray(@Nonnull T[] a) {
    //noinspection SuspiciousToArrayCall
    return resultsList.toArray(a);
  }

  @Override
  public boolean add(SolrResult solrResult) {
    return resultsList.add(solrResult);
  }

  @Override
  public boolean remove(Object o) {
    return resultsList.remove(o);
  }

  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    return resultsList.containsAll(c);
  }

  @Override
  public boolean addAll(@Nonnull Collection<? extends SolrResult> c) {
    return resultsList.addAll(c);
  }

  @Override
  public boolean addAll(int index, @Nonnull Collection<? extends SolrResult> c) {
    return resultsList.addAll(index, c);
  }

  @Override
  public boolean removeAll(@Nonnull Collection<?> c) {
    return resultsList.removeAll(c);
  }

  @Override
  public boolean retainAll(@Nonnull Collection<?> c) {
    return resultsList.retainAll(c);
  }

  @Override
  public void replaceAll(UnaryOperator<SolrResult> operator) {
    resultsList.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super SolrResult> c) {
    resultsList.sort(c);
  }

  @Override
  public void clear() {
    resultsList.clear();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return resultsList.equals(o);
  }

  @Override
  public int hashCode() {
    return resultsList.hashCode();
  }

  @Override
  public SolrResult get(int index) {
    return resultsList.get(index);
  }

  @Override
  public SolrResult set(int index, SolrResult element) {
    return resultsList.set(index, element);
  }

  @Override
  public void add(int index, SolrResult element) {
    resultsList.add(index, element);
  }

  @Override
  public SolrResult remove(int index) {
    return resultsList.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return resultsList.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return resultsList.lastIndexOf(o);
  }

  @Override
  public @Nonnull ListIterator<SolrResult> listIterator() {
    return resultsList.listIterator();
  }

  @Override
  public @Nonnull ListIterator<SolrResult> listIterator(int index) {
    return resultsList.listIterator(index);
  }

  @Override
  public @Nonnull List<SolrResult> subList(int fromIndex, int toIndex) {
    return resultsList.subList(fromIndex, toIndex);
  }
}
