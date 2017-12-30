package edu.utdallas.hltri.eeg.al;

import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Pair;

import java.util.List;

/**
 * Created by rmm120030 on 7/13/16.
 */
public interface ActiveLearner<A, D extends BaseDocument> extends Annotator<D> {

//  /**
//   * Retrain the model on the passed training data
//   * @param trainingData a List of annotations on which to retrain
//   * @param modelFile the full path of the file to save the retrained model to
//   */
//  void retrain(List<A> trainingData, String modelFile);

  /**
   * Annotate the document passed and return a list of Pairs of annotations and the annotation's confidence vector
   * @param document the document to be annotated
   * @param <B> DocumentType
   * @return a list of Pairs of annotations and the annotation's confidence vector
   */
  <B extends D> List<Pair<A,double[]>> annotateWithConfidence(final Document<B> document);
}
