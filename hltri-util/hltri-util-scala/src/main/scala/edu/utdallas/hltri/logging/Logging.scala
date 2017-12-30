package edu.utdallas.hltri.logging

import org.slf4j.{Logger => SLF4JLogger}

import scala.reflect.ClassTag
import scala.reflect.classTag

/** Scala front-end to a SLF4J logger. */
class LazyLogger(val logger: SLF4JLogger) extends Logger(logger) {

  /** Get the name associated with this logger.
    *
    * @return the name.*/
  @inline final def name: String = logger.getName

  /** Issue a trace logging message.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.*/
  @inline final def trace(msg: => Any): Unit =
    if (isTraceEnabled) logger.trace(msg.toString)

  /** Issue a trace logging message, with an exception.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.
    * @param t    the exception to include with the logged message.*/
  @inline final def trace(msg: => Any, t: => Throwable): Unit =
    if (isTraceEnabled) logger.trace(msg, t)

  /** Issue a debug logging message.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.*/
  @inline final def debug(msg: => Any): Unit =
    if (isDebugEnabled) logger.debug(msg.toString)

  /** Issue a debug logging message, with an exception.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.
    * @param t    the exception to include with the logged message.*/
  @inline final def debug(msg: => Any, t: => Throwable): Unit =
    if (isDebugEnabled) logger.debug(msg, t)

  /** Issue a trace logging message.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.*/
  @inline final def error(msg: => Any): Unit =
    if (isErrorEnabled) logger.error(msg.toString)

  /** Issue a trace logging message, with an exception.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.
    * @param t    the exception to include with the logged message.*/
  @inline final def error(msg: => Any, t: => Throwable): Unit =
    if (isErrorEnabled) logger.error(msg, t)

  /** Issue a trace logging message.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.*/
  @inline final def info(msg: => Any): Unit =
    if (isInfoEnabled) logger.info(msg.toString)

  /** Issue a trace logging message, with an exception.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.
    * @param t    the exception to include with the logged message.*/
  @inline final def info(msg: => Any, t: => Throwable): Unit =
    if (isInfoEnabled) logger.info(msg, t)

  /** Issue a trace logging message.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.*/
  @inline final def warn(msg: => Any): Unit =
    if (isWarnEnabled) logger.warn(msg.toString)

  /** Issue a trace logging message, with an exception.
    *
    * @param msg  the message object. `toString()` is called to convert it
    *             to a loggable string.
    * @param t    the exception to include with the logged message.*/
  @inline final def warn(msg: => Any, t: => Throwable): Unit =
    if (isWarnEnabled) logger.warn(msg, t)

  /** Converts any type to a String. In case the object is null, a null
    * String is returned. Otherwise the method `toString()` is called.
    *
    * @param msg  the message object to be converted to String
    *
    * @return the String representation of the message.*/
  private implicit def _any2String(msg: Any): String =
    msg match {
      case null => "<null>"
      case _ => msg.toString
    }
}

/** Mix the `Logging` trait into a class to get:
  *
  * - Logging methods
  * - A `Logger` object, accessible via the `log` property
  *
  * Does not affect the public API of the class mixing it in. */
trait Logging {
  /** Get the `Logger` for the class that mixes this trait in. The `Logger`
    * is created the first time this method is call. The other methods (e.g.,
    * `error`, `info`, etc.) call this method to get the logger.
    *
    * @return the `Logger`*/
  protected lazy val log = Loggers(getClass)
}

/** A factory for retrieving an SLF4JLogger. */
object Loggers {

  /** The name associated with the root logger. */
  val RootLoggerName = SLF4JLogger.ROOT_LOGGER_NAME

  /** Get the logger with the specified name. Use `RootName` to get the
    * root logger.
    *
    * @param name  the logger name
    *
    * @return the `Logger`.*/
  def apply(name: String): Logger =
    new Logger(org.slf4j.LoggerFactory.getLogger(name))

  /** Get the logger for the specified class, using the class's fully
    * qualified name as the logger name.
    *
    * @param cls  the class
    *
    * @return the `Logger`.*/
  def apply(cls: Class[_]): Logger = apply(cls.getName)

  /** Get the logger for the specified class type, using the class's fully
    * qualified name as the logger name.
    *
    * @return the `Logger`.*/
  def apply[C: ClassTag](): Logger = apply(classTag[C].runtimeClass.getName)

  /** Get the root logger.
    *
    * @return the root logger*/
  def rootLogger: Logger = apply(RootLoggerName)
}
