package edu.utdallas.hltri.io;

/**
 *
 * @author travis
 */
@SuppressWarnings("unused")
public class ANSIColors {

  // Enums for all bash-supported ANSI colors
  public static enum Style {

    NORMAL(0),
    BRIGHT(1);
    int value;

    Style(int value) {
      this.value = value;
    }
  }
  public static enum Color {

    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37);
    int value;

    Color(int value) {
      this.value = value;
    }
  }

  // Bash colorizing constants
  private static final String PREFIX = "\u001b[";
  private static final String SUFFIX = "m";
  private static final char SEPARATOR = ';';
  private static final String END_COLOR = PREFIX + SUFFIX;

  /* This variable is the same as the --color option in most linux tools
   * By default it behaves like "auto": disabling color if output is redirected
   * Setting to true behaves like "always": input is always output with color
   * Setting to false behaves like "never": input is never colored
   */
  public static boolean enabled = System.console() != null;

  public static String color(Style style, Color color, String text) {
    if (enabled) {
      return start(style, color) + text + end();
    } else {
      return text;
    }
  }

  public static String fcolor(Style style, Color color, String text) {
    return start(style, color) + text + end();
  }

  public static String start(Style style, Color color) {
    return PREFIX + style.value + SEPARATOR + color.value + SUFFIX;

  }

  public static String end() {
    return END_COLOR;
  }

  private ANSIColors() {
  }
}
