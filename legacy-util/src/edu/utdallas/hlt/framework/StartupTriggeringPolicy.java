package edu.utdallas.hlt.framework;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author travis
 */
@NoAutoStart
public class StartupTriggeringPolicy<E> extends SizeAndTimeBasedFNATP<E> {
  private final AtomicBoolean trigger = new AtomicBoolean();

  @Override
  public boolean isTriggeringEvent(final File activeFile, final E event) {
    if (trigger.compareAndSet(false, true) && activeFile.length() > 0) {
      String maxFileSize = getMaxFileSize();
      setMaxFileSize("1");
      super.isTriggeringEvent(activeFile, event);
      setMaxFileSize(maxFileSize);
      return true;
    }
    return super.isTriggeringEvent(activeFile, event);
  }
}
