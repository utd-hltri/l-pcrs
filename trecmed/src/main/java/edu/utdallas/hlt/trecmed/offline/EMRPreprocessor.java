
package edu.utdallas.hlt.trecmed.offline;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.Sentence;
import edu.utdallas.hlt.text.Token;
import edu.utdallas.hlt.text.io.XMLDocumentWriter;
import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hlt.util.io.IOUtil;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class EMRPreprocessor {

  Logger log = Logger.get(EMRPreprocessor.class);
  private final SAXBuilder builder = new SAXBuilder();

  public void preprocess(Path inputPath, Path outputPath) {
    for (Place place : IOUtil.deepFiles(Place.fromFile(inputPath.toString()))) {
      try {
        final Element visit = builder.build(place.toFile()).getRootElement();
        final String visitId = visit.getChildText("visitid");
        final Map<String, String> meta = new HashMap<>();
        for (final Element report : visit.getChildren("report")) {
          try {
            final String checksum = report.getChildText("checksum");
            File dest = outputPath.resolve(Paths.get(
                URLEncoder.encode(visitId, "UTF-8"),
                URLEncoder.encode(checksum, "UTF-8") + ".xml.gz")).toFile();
//            if (!dest.exists()) {
              dest.getParentFile().mkdirs();

              meta.put("subtype", report.getChildText("subtype"));
              meta.put("type", report.getChildText("type"));
              meta.put("chief_complaint", report.getChildText("chief_complaint"));
              meta.put("admit_diagnosis", report.getChildText("admit_diagnosis"));
              meta.put("discharge_diagnosis", report.getChildText("discharge_diagnosis"));
              meta.put("year", report.getChildText("year"));
              meta.put("update_time", report.getChildText("update_time"));
              meta.put("visitid", visitId);

              log.debug("Writing {} to {}", place, dest);
              Document doc = new Document(report.getChildText("report_text"));
              doc.getMetaDataMap().putAll(meta);
              doc.annotate(Token.TYPE);
//              doc.annotate(Token.STEM_TYPE);
              doc.annotate(Sentence.TYPE);
//              doc.annotate(Gender.TYPE);
//              doc.annotate(NegationSpan.TYPE);
//              doc.annotate(HedgeSpan.TYPE);
//              doc.annotate(Dependency.TYPE);
//              doc.annotate(PhraseChunk.TYPE);
              doc.setDocumentID(checksum);

              new XMLDocumentWriter().write(doc, Place.fromFile(dest));
//            }
          } catch (IOException ex) {
            log.error("Error while parsing report in visit " + visitId + ":", ex);
          }
        }
        log.info("Finished processing visit {}.", visitId);
      } catch (JDOMException | IOException ex) {
        log.error("Failed to parse file " + place + ":", ex);
      }
    }
  }

  public static void main(String... args) {
    args = App.init(args);
    new EMRPreprocessor().preprocess(Paths.get(args[0]), Paths.get(args[1]));
  }
}
