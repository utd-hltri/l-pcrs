package edu.utdallas.hltri.ml.classify;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.label.IobLabel;

/**
 * This class is used to process IOB sequence labels, one by one, combining them into chunks denoted by contiguous
 * sequences of B and optional I's.
 *
 * i.e.
 * O O O O B O O B I I O O
 * . . . .[ ]. .[     ]. .
 *
 * Created by rmm120030 on 4/17/17.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class IobSequenceChunker<T> extends IoSequenceChunker<T> {
  private static final Logger log = Logger.get(IobSequenceChunker.class);

  /**
   * Process begin token
   * @param token token
   * @param fineGrainedLabel fine grained label
   */
  protected void begin(T token, String fineGrainedLabel) {
    // if already in a span, do combine on previous span
    if (inSpan) {
      doCombine(start, end, this.fineGrainedLabel);
    }
    // start new span
    this.start = getStart(token);
    this.end = getEnd(token);
    this.fineGrainedLabel = fineGrainedLabel;
    this.inSpan = true;
  }

  protected void begin(T token) {
    inside(token, "");
  }

  /**
   * Updates the state of this object by adding the passed token with the passed IobLabel
   * @param token the next token in the sequence
   * @param label the label of the next token in the sequence
   */
  public void processToken(T token, IobLabel label) {
    switch(label) {
      case I: inside(token);
        break;
      case O: outside();
        break;
      case B: begin(token);
        break;
      default: throw new RuntimeException("Invalid IobLabel: " + label);
    }
  }

  /**
   * Updates the state of this object by adding the passed token with the passed IobLabel
   * @param token the next token in the sequence
   * @param label the label of the next token in the sequence
   * @param fineGrainedLabel fine grained label of the token (e.g. event or person from I-event, I-person)
   */
  public void processToken(T token, IobLabel label, String fineGrainedLabel) {
    switch(label) {
      case I: inside(token, fineGrainedLabel);
        break;
      case O: outside();
        break;
      case B: begin(token, fineGrainedLabel);
        break;
      default: throw new RuntimeException("Invalid IobLabel: " + label);
    }
  }

  /**
   * Process inside token.
   * Valid 'inside' tokens must occur after either a 'begin' token or another 'inside' token.
   * Invalid 'inside' tokens are treated as 'begin' tokens by this method. Use insideFailIfInvalid() if this is not
   * desired behavior.
   * @param token token
   * @param fineGrainedLabel fine grained label of the token (e.g. event or person from I-event, I-person)
   */
  protected void inside(T token, String fineGrainedLabel) {
    // valid calls to inside() require that we are in a span
    if (inSpan && fineGrainedLabel.equals(this.fineGrainedLabel)) {
      this.end = getEnd(token);
    }
    // if invalid call to inside(), treat it as a begin()
    else {
      log.warn("I without B: {}/{}. Treating as B", token, fineGrainedLabel);
      this.start = getStart(token);
      this.end = getEnd(token);
      this.fineGrainedLabel = fineGrainedLabel;
      this.inSpan = true;
    }
  }

  /**
   * Process inside token, throwing an exception if an inside token is not valid given the current state of the sequence
   * @param token token
   * @param fineGrainedLabel fine grained label of the token (e.g. event or person from I-event, I-person)
   */
  protected void insideFailIfInvalid(T token, String fineGrainedLabel) {
    // valid calls to inside() require that we are in a span
    if (inSpan && fineGrainedLabel.equals(this.fineGrainedLabel)) {
      this.end = getEnd(token);
    }
    // if invalid call to inside(), treat it as a begin()
    else {
      throw new RuntimeException("Invalid call to inside() on token [" + token +
          "]. Previous token fineGrainedLabel must be either begin() or inside()");
    }
  }
}
