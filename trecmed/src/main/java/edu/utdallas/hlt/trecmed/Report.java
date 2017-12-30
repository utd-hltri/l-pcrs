package edu.utdallas.hlt.trecmed;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.Sentence;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.text.TextComparators;
import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hltri.inquire.lucene.HasLuceneId;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class Report implements TRECDocument, HasLuceneId {
  private static final Logger log = Logger.get(Report.class);

  private String type;
  private String subtype;

  private final Visit visit;
  private final Place place;
  private final String id;
  private final int luceneId;

  private Document document;

  public String getType() {
    if (type == null) {
      if (document == null) {
        type = LuceneEMRSearcher.getReportSearcher().withReader(reader -> {
          try {
            return reader.document(luceneId).get("type");
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
      } else {
        type = getDocument().getMetaDataMap().get("type");
      }
    }
    return type;
  }

  public String getSubtype() {
    if (subtype == null) {
      if (document == null) {
        subtype = LuceneEMRSearcher.getReportSearcher().withReader(reader -> {
          try {
            return reader.document(luceneId).get("subtype");
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
      } else {
        subtype = getDocument().getMetaDataMap().get("subtype");
      }
    }
    return subtype;
  }

  public String getId() { return id; }


  private static String decode(final Place place) {
    try {
      return URLDecoder.decode(place.getName().replaceAll(".xml.gz", ""), "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String encode(final String checksum) {
    try {
      return URLEncoder.encode(checksum, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public Report(final Place place, final Visit visit) {
    this(place, visit, Report.decode(place));
  }

  protected Report(final Place place, final Visit visit, final String id) {
    this.place = place;
    this.visit = visit;
    this.id = id;
    this.luceneId = LuceneEMRSearcher.getReportSearcher().getDocId(getId());
  }

  static Report fromChecksum(final Visit visit, final String checksum) {

    return new Report(Place.fromFile(Config.get(Visit.class, "PATH") + "/" + visit.getEncodedId() + "/" + encode(checksum) + ".xml.gz"), visit, checksum);
  }

  public Report setType(final String type) {
    this.type = type;
    return this;
  }

  public Document getDocument() {
    if (document == null) {
      document = App.readReport(place);
    }
    return document;
  }

  @Override
  public int getLuceneId() {
    return this.luceneId;
  }

  public Sentence getPreviousSection(Text text) {
    Sentence sentence = text.getOnlySuper(Sentence.class);
    if (sentence == null) {
      List<Sentence> sentences = new ArrayList<>(text.getIntersecting(Sentence.class));
      sentences.sort(TextComparators.startToken());
      sentence = sentences.get(0);
    }
    if (!sentence.hasPrevToken()) { return sentence; }
    text = sentence.getPrevToken();
    do {
      sentence = text.getOnlySuper(Sentence.class);
      if (!sentence.hasPrevToken()) { break; }
      text = sentence.getPrevToken().getPrevToken();
    } while (!sentence.asRawString().endsWith(":"));
    return sentence;
  }

  @Override public String toString() {
    return getId() + "(" + "#" + visit.getId() + ")";
  }

  public static Report fromLucene(org.apache.lucene.document.Document doc, int luceneId) {
      return Report.fromChecksum(Visit.fromId(doc.get(LuceneEMRSearcher.VISIT_ID_FIELDNAME)),
                                doc.get(LuceneEMRSearcher.CHECKSUM_FIELDNAME));
  }

  public void close() {
    this.document = null;
  }
}
