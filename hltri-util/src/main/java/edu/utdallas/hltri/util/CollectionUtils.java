package edu.utdallas.hltri.util;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {

  public static <E, T extends Collection<? extends E>> Collection<E> flattenToList(
      Collection<T> collections) {
    return collections.stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  public static <E, T extends Collection<? extends E>> Collection<E> flattenToSet(
      Collection<T> collections) {
    return collections.stream().flatMap(Collection::stream).collect(Collectors.toSet());
  }

  /**
   * Turns an Optional<T> into a Stream<T> of length zero or one depending upon
   * whether a value is present.
   */
  public static <T> Stream<T> streamOptional(Optional<T> opt) {
    if (opt.isPresent())
      return Stream.of(opt.get());
    else
      return Stream.empty();
  }
}
