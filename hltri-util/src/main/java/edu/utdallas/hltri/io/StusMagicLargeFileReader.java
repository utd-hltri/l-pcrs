package edu.utdallas.hltri.io;

/** Taken from: http://stackoverflow.com/questions/736556/binary-search-in-a-sorted-memory-mapped-file-in-java */

import edu.utdallas.hltri.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

/**
 * When operating on >2GB files this must be run in a 64bit jvm
 * @author bryan
 */
public class StusMagicLargeFileReader implements AutoCloseable {
  private static final Logger                 log       = Logger.get();
  private static final long                   PAGE_SIZE = Integer.MAX_VALUE;
  private              List<MappedByteBuffer> buffers   = new ArrayList<>();
  private final        byte                   raw[]     = new byte[1];
  long position = 0;
  long        fileLength;
  FileChannel channel;

  public static void main(String[] args) throws IOException {
    File file = new File("/Users/stu/test.txt");
    try (FileChannel fc = (new RandomAccessFile(file, "r")).getChannel();
         StusMagicLargeFileReader buffer = new StusMagicLargeFileReader(fc)) {
      long position = file.length() / 2;
      String candidate = buffer.getString(position--);
      while (position >= 0 && !candidate.equals('\n'))
        candidate = buffer.getString(position--);
      //have newline position or start of file...do other stuff
    }
  }

  public StusMagicLargeFileReader(File file) throws IOException {
      this(new RandomAccessFile(file, "r").getChannel());
    }

    StusMagicLargeFileReader(FileChannel channel) throws IOException {
        long start = 0, length = 0;
        for (long index = 0; start + length < channel.size(); index++) {
            if ((channel.size() / PAGE_SIZE) == index)
                length = (channel.size() - index *  PAGE_SIZE) ;
            else
                length = PAGE_SIZE;
            start = index * PAGE_SIZE;
            buffers.add((int) index, channel.map(MapMode.READ_ONLY, start, length));
        }
        this.fileLength = channel.size();
        this.channel = channel;
    }
    public String getString(long bytePosition) {
        raw[0] = getByte(bytePosition);
        return new String(raw);
    }

     public byte getByte(long bytePosition) {

       int page = (int) (bytePosition / PAGE_SIZE);
       int index = (int) (bytePosition % PAGE_SIZE);
       try {
         return buffers.get(page).get(index);
       } catch (IndexOutOfBoundsException ex) {
         log.error("Index " + index + " out bounds! Are you sure your file is sorted?", ex);
         throw ex;
       }
    }

    public void seek(long newPos) {
      this.position = newPos;
    }

    public byte readByte() {
      return getByte(position++);
    }

    public String readLine() {
      byte[] data = new byte[10];
      int index = 0;
      byte next = 0;
      while (position < fileLength && ((next = readByte()) != '\n')) {
        if (index >= data.length) {
          byte[] newData = new byte[data.length * 2];
          System.arraycopy(data, 0, newData, 0, data.length);
          data = newData;
        }
        data[index++] = next;
      }
      return new String(data, 0, index);
    }

    @Override public void close() throws IOException {
      channel.close();
    }

    public long getLength() {
      return fileLength;
    }

    public long getPosition() {
      return position;
    }
}
