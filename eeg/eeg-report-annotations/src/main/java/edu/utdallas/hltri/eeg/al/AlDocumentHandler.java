package edu.utdallas.hltri.eeg.al;

import edu.utdallas.hltri.scribe.text.BaseDocument;

/**
 *
 * @param <A> the type of each example where an example is the thing we provide to the oracle
 * @param <D> document type
 */
public interface AlDocumentHandler<A, D extends BaseDocument> {

  void outputNextNExamples(int n);

  void processNewlyAnnotatedExamples();
}
