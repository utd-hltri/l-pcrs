package edu.utdallas.hlt.xml;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author bryan
 */
public abstract class ExtendedDefaultHandler extends DefaultHandler  {
    private StringBuilder buffer = new StringBuilder();
    ArrayList<String> stack = new ArrayList<>();

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
      stack.remove(stack.size()-1);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      buffer.append(ch, start, length);
    }

    public boolean stackMatch(String path) {
      if (path.startsWith("/")) { path = path.substring(1); }
      String[] parts = path.split("/");
      if (parts.length > stack.size()) { return false; }
      for (int i = 0; i < parts.length; i++) {
        if ( ! parts[parts.length-i-1].equals(stack.get(stack.size()-i-1))) {
          return false;
        }
      }
      return true;
    }
    
    public boolean stackEndsWith(String path) {
      if (path.startsWith("/")) { path = path.substring(1); }
      String[] parts = path.split("/");
      if (parts.length > stack.size()) { return false; }
      for (int i = 0; i < parts.length; i++) {
        if ( ! parts[i].equals(stack.get(stack.size() - parts.length + i))) {
          return false;
        }
      }
      return true;
    }

    public String getText() {
      return buffer.toString();
    }

    public String getCleanText() {
      return getText().trim();
    }

  public void end(String qName) {

  }

  public static void parseFile(File file, ExtendedDefaultHandler handler) {
    // Create a SAXParser to parse the XML document
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    try {
      SAXParser parser = factory.newSAXParser();

      // Parse document
      parser.parse(file, handler);
    } catch (ParserConfigurationException | SAXException | IOException saxe) {
      throw new RuntimeException(saxe);
    }
  }
}
