
package edu.utdallas.hlt.trecmed.offline;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.utdallas.hlt.i2b2.Concept;
import edu.utdallas.hlt.text.Dependency;
import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.Gender;
import edu.utdallas.hlt.text.HedgeSpan;
import edu.utdallas.hlt.text.NegationSpan;
import edu.utdallas.hlt.text.Token;
import edu.utdallas.hlt.text.io.XMLDocumentWriter;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hlt.util.Place;

/**
 *
 * @author travis
 */
public class QueryPreprocessor {

  public void preprocess(Path inputPath, Path outputPath) {
    try {
    List<Topic> questions = QueryParser.fromPath(inputPath);
    List<Document> documents = new ArrayList<>();
    for (final Topic q : questions) {
      System.out.println(q);
      Document d = new Document(q.asString());
      d.annotate(Token.STEM_TYPE);
      d.annotate(Dependency.TYPE);
      d.annotate(Gender.TYPE);
      d.annotate(NegationSpan.TYPE);
      d.annotate(HedgeSpan.TYPE);
      d.annotate(Concept.TYPE);
//      d.annotate(PhraseChunk.TYPE);
      d.setDocumentID(q.getId());
//      new edu.utdallas.hlt.metamap.MetaMapResultWrapper().annotateDocument(d);
      documents.add(d);
    }
    try {
      new XMLDocumentWriter()
//          .addAnnotationWriter(NEgexSpan.writer())
//          .addAnnotationWriter(MetaMapPhrase.writer())
//          .addAnnotationWriter(MetaMapUtterance.writer())
          .writeAll(documents, Place.fromFile(outputPath.toString()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String... args) {
    args = App.init(args);
    new QueryPreprocessor().preprocess(Paths.get(args[0]), Paths.get(args[1]));
  }
}
