package edu.utdallas.hlt.trecmed;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import org.apache.lucene.document.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hlt.util.Place.ExtensionFilter;
import edu.utdallas.hlt.util.io.IOUtil;
import edu.utdallas.hltri.inquire.lucene.HasLuceneId;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 *
 * @author travis
 */
public class Visit implements Iterable<Report>, TRECDocument, Identifiable, HasLuceneId {
  private static final Logger log = Logger.get(Visit.class);

  final private String visitId;
  final private int docId;

  public Visit(String visitId, int docId) {
    this.visitId = visitId;
    this.docId = docId;
  }

  public static Visit fromId(String id) {
    return new Visit(id, LuceneEMRSearcher.getVisitSearcher().getDocId(id));
  }

  public static Visit fromEncodedId(String id) {
    try {
      return fromId(URLDecoder.decode(id, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Visit fromLucene(Document doc, int luceneId) {
    return new Visit(doc.get(LuceneEMRSearcher.VISIT_ID_FIELDNAME), luceneId);
  }

  @Override
  public String getId() {
    return visitId;
  }

  @Override
  public int getLuceneId() {
    return docId;
  }

  public String getEncodedId() {
    try {
      return URLEncoder.encode(visitId,"UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }


  public String getLinkableId() {
    try {
      return URLEncoder.encode(URLEncoder.encode(visitId,"UTF-8"), "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override public String toString() {
    return "Visit#'" +visitId + '\'';
  }

  @Override public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    final Visit other = (Visit) obj;
    if (!Objects.equals(this.visitId, other.visitId)) {
      return false;
    }
    return true;
  }

  @Override public int hashCode() {
    int hash = 7;
    hash = 67 * hash + Objects.hashCode(this.visitId);
    return hash;
  }

  private static Multimap<Visit, Report> mapping = null;

  public static Multimap<Visit, Report> getMapping() {
    if (mapping == null) {
      mapping = ArrayListMultimap.create();

      final Splitter splitter = Splitter.on('\t');
      Iterator<String> it;
      String key, type, value;
      Visit visit;
      try (BufferedReader reader = new BufferedReader(new FileReader(Config.get(Visit.class, "MAPPING").toString()))) {
        for (String line; (line = reader.readLine()) != null; ) {
          it = splitter.split(line).iterator();
          key = it.next();
          type = it.next();
          value = it.next();
          visit = Visit.fromId(value);
          mapping.put(visit, Report.fromChecksum(visit, key).setType(type));
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return mapping;
  }

  public Collection<Report> getLazyReports() {
    return getMapping().get(this);
  }

  public Multiset<String> getReportTypeDistribution() {
    Multiset<String> set = HashMultiset.create();
    for (Report report : getLazyReports()) {
      set.add(report.getType());
    }
    return set;
  }

  @Override public Iterator<Report> iterator() {
    return getReports().iterator();
  }

  @Deprecated public Iterable<Report> getReports() {
    try {
      return new ReportIterable(IOUtil.deepFiles(
        Place.fromFile(Config.get(Visit.class, "PATH").toString() +
                       File.separatorChar + URLEncoder.encode(visitId, "UTF-8")),
        new ExtensionFilter("xml.gz")),
                                this);
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static class ReportIterable implements Iterable<Report> {
    final Iterator<Place> places;
    final Visit           parent;

    ReportIterable(Iterable<Place> places, Visit parent) {
      this.places = places.iterator();
      this.parent = parent;
    }

    @Override public Iterator<Report> iterator() {
      return new Iterator<Report>() {
        @Override public boolean hasNext() {
          return places.hasNext();
        }

        @Override public Report next() {
          return new Report(places.next(), parent);
        }

        @Override public void remove() {
          throw new UnsupportedOperationException("Not supported.");
        }
      };
    }
  }

//  public static enum VisitLuceneFactory implements LuceneFactory<Visit> {
//    INSTANCE;
//
//    @Override public Visit build(Document doc, int luceneId) {
//      return new Visit(doc.get(LuceneEMRSearcher.VISIT_ID_FIELDNAME), luceneId);
//    }
//  }
}


