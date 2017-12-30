package edu.utdallas.hlt.medical.knowledge;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import edu.utdallas.hlt.text.StopWords;
import edu.utdallas.hlt.util.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 *
 * @author travis
 */
public class MedicalAbbreviationConverter {

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MedicalAbbreviationConverter.class);

  Map<String, String> abbreviations;
  final File abbreviationsFile;

  public MedicalAbbreviationConverter() {
    this.abbreviationsFile = new File(Config.get(MedicalAbbreviationConverter.class, "PATH").toString());
  }



  protected Map<String, String> getAbbreviations() {
    if (abbreviations == null) {
      try {
        abbreviations = Maps.newHashMap();
        LOGGER.info("Loading medical abbrevations from {}", abbreviationsFile.getName());

        BufferedReader reader = Files.newReader(abbreviationsFile, Charset.defaultCharset());
        String line;
        reader.readLine();
        while ((line = reader.readLine()) != null) {
          String[] cols = line.split("\t");

          if (cols.length < 2 || cols[0].equals(cols[1])) {
            continue;
          }

          boolean isValidEntry = true;
          for (int i = 0; i < 2; i++) {
            if (Strings.isNullOrEmpty(cols[i]) || StopWords.isStopWord(cols[i]))
              isValidEntry = false;
          }

          if (isValidEntry) {
            abbreviations.put(cols[0].toString(), cols[1].toString());
          }
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    return abbreviations;
  }

  public String convert(String term) {
    assert term != null;
    String result = getAbbreviations().get(term.toLowerCase());

    if (result == null)
      return term;

    LOGGER.trace("Convering {} to {}.", term, result);
    return result;
  }
}
