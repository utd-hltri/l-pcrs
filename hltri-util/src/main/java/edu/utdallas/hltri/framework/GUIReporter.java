package edu.utdallas.hltri.framework;

import javax.swing.*;
import java.awt.*;

/**
 * User: bryan
 * Date: 5/20/13
 * Time: 2:59 PM
 * Created with IntelliJ IDEA.
 */
public class GUIReporter extends JPanel {
  private final static long serialVersionUID = 1L;

  private final JProgressBar progressBar;
  private final JTextArea output;
  private final char newLine = '\n';

  public GUIReporter(final String name, final int max) {
    super(new BorderLayout());

    progressBar = new JProgressBar(0, max);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);

    output = new JTextArea(5, 20);
    output.setMargin(new Insets(5, 5, 5, 5));
    output.setEditable(false);
    output.setCursor(null); //inherit the panel's cursor
    //see bug 4851758

    JPanel panel = new JPanel();
    panel.add(progressBar);

    add(panel, BorderLayout.PAGE_START);
    add(new JScrollPane(output), BorderLayout.CENTER);
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override public void run() {
        final JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final GUIReporter self = GUIReporter.this;
        self.setOpaque(true);
        frame.setContentPane(self);

        frame.pack();
        frame.setVisible(true);
      }
    });
  }

  public void updateStatus(final int current, final String status) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override public void run() {
        progressBar.setValue(current);
        output.append(status);
      }
    });
  }

  public static void main(String... args) {
    GUIReporter test = new GUIReporter("Test", 500);
    for (int i = 0; i < 500; i++) {
      test.updateStatus(i, "Iteration " + i);
      try {
        Thread.sleep((long) (Math.random() * 1000));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
