package edu.utdallas.hltri.ml.classify;

import edu.utdallas.hltri.io.AC;
import edu.utdallas.hltri.ml.label.IoLabel;

/**
 * This class is used to process IO sequence labels, one by one, combining them into chunks denoted by contiguous
 * sequences of I's.
 *
 * i.e.
 * O O O O I O O I I I O O
 * . . . .[ ]. .[     ]. .
 *
 * Created by rmm120030 on 4/17/17.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class IoSequenceChunker<T> implements AC {
  protected int start = -1, end = -1;
  protected boolean inSpan = false;
  protected String fineGrainedLabel;

  /**
   * Combine should create a chunk from start to end, optionally with a fineGrainedLabel
   * @param start start
   * @param end end
   * @param label optional fine grained label used for IO sequences with more than one chunk type (e.g. I-event, I-person)
   */
  public abstract void combine(int start, int end, String label);

  /**
   * Function from a sequence unit (token) to its start offset
   * @param token token
   * @return start offset
   */
  public abstract int getStart(T token);

  /**
   * Function from a sequence unit (token) to its end offset
   * @param token token
   * @return end offset
   */
  public abstract int getEnd(T token);

  /**
   * Updates the state of this object by adding the passed token with the passed IoLabel
   * @param token the next token in the sequence
   * @param label the label of the next token in the sequence
   */
  public void processToken(T token, IoLabel label) {
    switch(label) {
      case I: inside(token);
        break;
      case O: outside();
        break;
      default: throw new RuntimeException("Invalid IoLabel: " + label);
    }
  }

  /**
   * Updates the state of this object by adding the passed token with the passed IoLabel
   * @param token the next token in the sequence
   * @param label the label of the next token in the sequence
   * @param fineGrainedLabel fine grained label of the token (e.g. event or person from I-event, I-person)
   */
  public void processToken(T token, IoLabel label, String fineGrainedLabel) {
    switch(label) {
      case I: inside(token, fineGrainedLabel);
        break;
      case O: outside();
        break;
      default: throw new RuntimeException("Invalid IoLabel: " + label);
    }
  }

  /**
   * Process inside token.
   * @param token token
   * @param fineGrainedLabel fine grained label (e.g. event or person from I-event, I-person)
   */
  protected void inside(T token, String fineGrainedLabel) {
    if (inSpan) {
      if (fineGrainedLabel.equals(this.fineGrainedLabel)) {
        end = getEnd(token);
      } else {
        doCombine(start, end, this.fineGrainedLabel);
        start = getStart(token);
        end = getEnd(token);
        this.fineGrainedLabel = fineGrainedLabel;
        inSpan = true;
      }
    } else {
      start = getStart(token);
      end = getEnd(token);
      this.fineGrainedLabel = fineGrainedLabel;
      inSpan = true;
    }
  }

  /**
   * Process inside token
   * @param token token
   */
  protected void inside(T token) {
    inside(token, "");
  }

  /**
   * Process outside token
   */
  protected void outside() {
    if (inSpan) {
      doCombine(start, end, fineGrainedLabel);
      start = -1;
      end = -1;
      fineGrainedLabel = null;
      inSpan = false;
    }
  }

  protected void doCombine(int start, int end, String fineGrainedLabel) {
    assert start < end : String.format("Invalid offsets {%d, %d}", start, end);
    assert start >= 0 : "Invalid start offset: " + start;
    assert end > 0: "invalid end offset: " + end;

    combine(start, end, fineGrainedLabel);
  }

  @Override
  public void close() {
    outside();
  }
}
