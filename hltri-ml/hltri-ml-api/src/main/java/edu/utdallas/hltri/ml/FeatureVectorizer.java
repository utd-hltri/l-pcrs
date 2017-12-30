package edu.utdallas.hltri.ml;

//import cc.mallet.types.ConstantMatrix;
//import cc.mallet.types.SparseVector;
//import cc.mallet.types.Vector;
import edu.utdallas.hltri.ml.Feature;

/**
 * Can return a vector holding the feature values for a training or test instance.
 * User: bryan
 * Date: 12/20/12
 * Time: 9:02 AM
 * Created with IntelliJ IDEA.
 */
public interface FeatureVectorizer {

//  public SparseVector getVector(Iterable<? extends Feature<?>> features, boolean growVectors);

  public int numDimensions();

  public String getFeatureRepresentation(int featID);
}
