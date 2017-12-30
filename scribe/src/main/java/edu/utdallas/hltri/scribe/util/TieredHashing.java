package edu.utdallas.hltri.scribe.util;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.File;

/**
 * Created by rmm120030 on 7/16/15.
 */
public abstract class TieredHashing {
  public static final int tiers = 2;
  public static final int seed = 1337;
  public static final HashFunction murmur3 = Hashing.murmur3_128(seed);

  public static File tieredFolder(String path, String id) {
    final HashCode code = murmur3.hashString(id, Charsets.UTF_8);
    final byte[] bytes = code.asBytes();

    File file = new File(path);
    for (int i = 0; i < tiers; i++) {
      file = new File(file, String.format("%02x", bytes[i]));
    }
    file = new File(file, id);
    file.mkdirs();
    return file;
  }

  public static File tieredFile(String path, String id, String suffix) {
    final HashCode code = murmur3.hashString(id, Charsets.UTF_8);
    final byte[] bytes = code.asBytes();

    File file = new File(path);
    for (int i = 0; i < tiers; i++) {
      file = new File(file, String.format("%02x", bytes[i]));
    }
    file.mkdirs();
    return new File(file, id + suffix);
  }

  /**
   * Returns the string representing the tiers number of folders created to hash the passed it.
   * ex: '/ab/cd'
   * @param id
   * @return
   */
  public static String getHashDirsAsString(String id) {
    final byte[] bytes = murmur3.hashString(id, Charsets.UTF_8).asBytes();
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("%02x", bytes[0]));
    for (int i = 1; i < tiers; i++) {
      sb.append(String.format("%s%02x", File.separator, bytes[i]));
    }

    return sb.toString();
  }

  public static void main(String... args) {
    for (String arg : args) {
      System.out.println(arg + " -> " + getHashDirsAsString(arg));
    }
  }
}
