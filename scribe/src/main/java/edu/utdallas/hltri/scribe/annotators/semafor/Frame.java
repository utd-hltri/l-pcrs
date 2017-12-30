package edu.utdallas.hltri.scribe.annotators.semafor;

import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import org.jdom2.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 9/6/14
 * Time: 12:57 PM
 */
public class Frame implements Serializable {
  private static final long serialVersionUID = 1l;
  private final String name;
  private final List<FrameElement> elements = new ArrayList<>();
  private FrameElement target;

  public Frame(Element annSet, Sentence sentence, String semSentence) {
    name = annSet.getAttributeValue("frameName");
    for (final Element layers : annSet.getChildren()) {
      if (layers.getName().equals("layers")) {
        for (final Element layer : layers.getChildren()) {
          if (layer.getName().equals("layer")) {
            for (final Element labels : layer.getChildren()) {
              if (labels.getName().equals("labels")) {
                for (final Element label : labels.getChildren()) {
                  if (label.getName().equals("label")) {
                    FrameElement element = new FrameElement(label, sentence, semSentence);
                    elements.add(element);
                    if (element.getName().equalsIgnoreCase("target")) {
                      if (target == null)
                        target = element;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public FrameElement getTarget() {
    if (target != null) {
      return target;
    }
    else {
      throw new RuntimeException("No target for frame: " + name);
    }
  }

  public String getName() {
    return name;
  }

  public List<FrameElement> getElements() {
    return elements;
  }
}
