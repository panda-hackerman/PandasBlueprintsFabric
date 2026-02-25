package dev.michaud.pandas_blueprints.util;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;

public class VarIntUtil {

  /**
   * Write a variable-length int to the stream (using as few bytes as possible). The most
   * significant bit is reserved to signify if the following byte is part of the same value.
   *
   * @param value The value to write.
   * @param out   The stream to write to.
   * @throws IOException If an I/O error occurs.
   */
  public static void writeVarInt(int value, OutputStream out) throws IOException {
    while ((value & ~0x7f) != 0) {
      out.write((value & 0x7F) | 0x80);
      value >>>= 7;
    }

    out.write(value);
  }

  /**
   * Read a variable length int from the stream.
   *
   * @param in The buffer to read from.
   * @return The decoded int.
   * @throws IOException If the stream ended prematurely or the input was malformed (e.g. >5 bytes
   *                     for a single int).
   */
  public static int readVarInt(InputStream in) throws IOException {
    int value = 0;

    for (int i = 0; i < 5; i++) {

      final int b = in.read();
      if (b == -1) {
        throw new EOFException("Unexpected end of stream reading VarInt!");
      }

      value |= (b & 0x7f) << (i * 7);

      if ((b & 0x80) == 0) {
        return value; // That was the last byte
      }
    }

    throw new IOException("VarInt too big: length must be <= 5 bytes, data is likely corrupted!");
  }

  public static void writeVarInts(Collection<Integer> values, OutputStream out) throws IOException {
    for (int val : values) {
      writeVarInt(val, out);
    }
  }

  public static byte[] toVarIntArray(Collection<Integer> values) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try {
      writeVarInts(values, buffer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return buffer.toByteArray();
  }

}