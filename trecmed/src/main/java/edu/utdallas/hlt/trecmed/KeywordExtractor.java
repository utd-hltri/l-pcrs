package edu.utdallas.hlt.trecmed;

/**
 *
 * @author travis
 */
@FunctionalInterface
public interface KeywordExtractor {
  Iterable<Keyword> extract(Topic topic);
}
