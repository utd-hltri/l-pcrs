package edu.utdallas.hltri.scribe.annotators.semafor;

import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import org.jdom2.Element;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 9/6/14
 * Time: 12:58 PM
 */
public class FrameElement implements Serializable {
  private static final long serialVersionUID = 1l;
  private final String name;
  private final long start;
  private final long end;

  public FrameElement (Element label, Sentence sentence, String semSentence) {
    name = label.getAttributeValue("name");
    final String word = semSentence.substring(Integer.parseInt(label.getAttributeValue("start")),
        Integer.parseInt(label.getAttributeValue("end")));

    start = sentence.get(Token.StartOffset) + sentence.asString().indexOf(word);
    end = start + (Long.parseLong(label.getAttributeValue("end")) - Long.parseLong(label.getAttributeValue("start"))) + 1L;
  }

  public String getName() {
    return name;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }
}
