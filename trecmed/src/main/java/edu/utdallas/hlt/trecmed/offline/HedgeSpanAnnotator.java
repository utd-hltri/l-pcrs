package edu.utdallas.hlt.trecmed.offline;

import edu.utdallas.hlt.text.Annotation;
import edu.utdallas.hlt.text.HedgeSpan;
import edu.utdallas.hlt.text.NegationSpan;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.util.Config;

/**
* Annotates a {@link NegationSpan} using LingScope.
* @author travis
*/
public class HedgeSpanAnnotator extends LingScopeAnnotator {
  public HedgeSpanAnnotator() {
    super(Config.get(HedgeSpanAnnotator.class, "hedgeModel").toString());
  }

  @Override protected Annotation getSpan(Text text) {
    return new HedgeSpan(text);
  }
}
