package edu.utdallas.hltri.io;

import com.google.common.collect.Iterables;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author bryan
 */
public abstract class ExtendedDefaultHandler extends DefaultHandler  {
  private StringBuilder buffer = new StringBuilder();
  private ArrayList<String> stack = new ArrayList<String>();

  @Override
  public final void startElement(String nsURI, String strippedName,
                                 String tagName, Attributes attributes)
      throws SAXException {
    stack.add(tagName);
    start(tagName, attributes);
  }

  public void start(String tagname, Attributes attributes) {

  }

  @Override
  public final void endElement (String uri, String localName, String qName) {
    end(qName);
    buffer.setLength(0);
    stack.remove(stack.size() - 1);
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    buffer.append(ch, start, length);
  }

  public boolean stackMatch(String... path) {
    return Iterables.elementsEqual(Arrays.asList(path), stack);
  }

  public String getText() {
    return buffer.toString();
  }

  public String getCleanText() {
    return getText().trim();
  }

  public void clearBuffer() {
    buffer.setLength(0);
  }

  public void end(String qName) {

  }
}
