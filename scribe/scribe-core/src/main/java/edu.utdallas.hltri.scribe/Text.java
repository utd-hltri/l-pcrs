package edu.utdallas.hltri.scribe;

import java.util.List;

import edu.utdallas.hltri.util.RangeMap;

/**
 * Created by trg19 on 8/27/2016.
 */
@SuppressWarnings("unused")
public class Text extends Span implements AnnotationSet {
  private static final long serialVersionUID = 1L;

  final RangeMap<? extends Annotation> annotations = new RangeMap<>();

  public Text(String body) {
    super(body);
  }

  @Override
  public List<? extends Annotation> getAnnotations() {
    return annotations.toSortedList();
  }
}
