package edu.utdallas.hltri.scribe;

import javax.annotation.Nonnull;

/**
 * Created by rmm120030 on 7/13/15.
 */
public interface Identifiable {
  @Nonnull String getId();
  long getNumericId();
}
