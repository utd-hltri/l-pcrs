package edu.utdallas.hltri.ml;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by ramon on 10/20/2016.
 */
@FunctionalInterface
public interface FeatureExtractor<I,O> extends Function<I, Stream<? extends Feature<O>>> {
}
