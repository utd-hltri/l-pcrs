package edu.utdallas.hlt.trecmed.offline;

import edu.utdallas.hlt.text.Annotation;
import edu.utdallas.hlt.text.NegationSpan;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.util.Config;

/**
* Annotates a {@link NegationSpan} using LingScope.
* @author travis
*/
public class NegationSpanAnnotator extends LingScopeAnnotator {
  public NegationSpanAnnotator() {
    super(Config.get(NegationSpanAnnotator.class, "negationModel").toString());
  }

  @Override
  protected Annotation getSpan(Text text) {
    return new NegationSpan(text);
  }
}