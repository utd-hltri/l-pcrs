package edu.utdallas.hltri.mercury.api;

import com.google.common.base.Splitter;
import edu.utdallas.hltri.logging.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by rmm120030 on 5/31/17.
 */
public class ApiKeyManager {
  private static final Logger log = Logger.get(ApiKeyManager.class);

  private final Map<String, Integer> accessMap;

  public ApiKeyManager(String apiKeyFile) {
    accessMap = new HashMap<>();
    final Splitter splitter = Splitter.on("\t").omitEmptyStrings();
    try (BufferedReader reader = new BufferedReader(new FileReader(apiKeyFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.trim().isEmpty()) {
          final Iterator<String> split = splitter.split(line).iterator();
          accessMap.put(split.next(), Integer.parseInt(split.next()));
        }
      }
      log.info("Loaded {} apikeys from {}", accessMap.size(), apiKeyFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean hasAccess(String apikey) {
    return accessMap.containsKey(apikey);
  }

  public boolean hasAdminAccess(String apikey) {
    return hasAccess(apikey) && accessMap.get(apikey).equals(1);
  }
}
