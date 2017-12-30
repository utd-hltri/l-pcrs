package edu.utdallas.hltri.scribe.annotators;

import com.google.common.collect.Sets;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.Language;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.heideltime.standalone.components.impl.XMIResultFormatter;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import edu.utdallas.hlt.util.xml.XMLUtil;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.kirk.KirkDocument;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Timex3;
import org.jdom2.Element;

import java.util.Date;
import java.util.Set;

/**
 * Created by rmm120030 on 4/13/16.
 */
public class HeidelTimeAnnotator<D extends BaseDocument> implements Annotator<D>{
  private static final Logger log = Logger.get(HeidelTimeAnnotator.class);
  public static final String ANNOTATION_SET = "heideltime";

  private final HeidelTimeStandalone heidelTime;
  private final ResultFormatter formatter;
  private boolean clear = false;

  public HeidelTimeAnnotator() {
    heidelTime = new HeidelTimeStandalone(Language.ENGLISH, DocumentType.NEWS, OutputType.TIMEML);
    formatter = new XMIResultFormatter();
  }

  public HeidelTimeAnnotator clear() {
    clear = true;
    return this;
  }

  @Override
  public <B extends D> void annotate(final Document<B> document) {
    if (clear) {
      document.clear(ANNOTATION_SET);
    }

    final edu.utdallas.hlt.text.Document kdoc = KirkDocument.asKirk(document, "genia", "opennlp");

    final String docString = kdoc.asString();
    final Date date = new Date();

    final String result;
    try {
      result = heidelTime.process(docString, date, formatter);
    }
    catch (DocumentCreationTimeMissingException dctme) {
      throw new RuntimeException(dctme);
    }

    final Element root = XMLUtil.getDocument(result).getRootElement();
    final Set<String> idSet = Sets.newHashSet();
    assert root.getName().equals("XMI");
    for (final Element child1 : XMLUtil.getChildren(root)) {
      if (child1.getName().equals("Timex3")) {
        final int begin = Integer.valueOf(child1.getAttributeValue("begin"));
        final int end = Integer.valueOf(child1.getAttributeValue("end"));
        final String timexId = child1.getAttributeValue("timexId");
        final String timexInstance = child1.getAttributeValue("timexInstance");
        final String timexType = child1.getAttributeValue("timexType");
        final String timexValue = child1.getAttributeValue("timexValue");
        final String timexQuant = child1.getAttributeValue("timexQuant");
        final String timexFreq = child1.getAttributeValue("timexFreq");
        final String timexMod = child1.getAttributeValue("timexMod");
        assert idSet.add(timexId) : "More than one Timex for " + timexId;
        assert timexInstance.equals("0");

        final Timex3 timex = Timex3.TYPE.create(document, ANNOTATION_SET, begin, end);
        timex.set(Timex3.tid, timexId);
        timex.set(Timex3.type, timexType);
        timex.set(Timex3.value, timexValue);
        if (timexMod.length() > 0) {
          timex.set(Timex3.mod, timexMod);
        }
        if (timexFreq.length() > 0) {
          timex.set(Timex3.freq, timexFreq);
        }
        if (timexQuant.length() > 0) {
          timex.set(Timex3.quant, timexQuant);
        }
        log.info("Found timex3: {}", timex.get(Timex3.type));
      }
    }
  }
}
