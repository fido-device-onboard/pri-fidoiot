// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utility class for Buffer operations.
 */
public class BufferUtils {

  /**
   * Converts a BytesBuffer to a byte array.
   *
   * @param buffer The ByteBuffer to convert.
   * @return The ByteBuffer as a byte array..
   */
  public static byte[] unwrap(ByteBuffer buffer) {
    if (buffer.hasArray()
        && buffer.remaining() == buffer.array().length
        && buffer.position() == 0) {
      return buffer.array();
    }
    byte[] cpy = new byte[(buffer.remaining())];
    buffer.get(cpy);
    return cpy;
  }

  /**
   * Write a BigInteger into a fixed-length buffer.
   *
   * @param src     Source Integer to write to the buffer.
   * @param dest    Destination buffer.
   * @param destPos Position to write in destination buffer
   * @param length  The exact length to write (truncate or pad to fix).
   */
  public static void writeBigInteger(BigInteger src, byte[] dest, int destPos, int length) {
    byte[] intbuf = src.toByteArray(); // min #bits, with one sign bit guaranteed!
    int byteLen = src.bitLength() / Byte.SIZE + 1;

    if (byteLen >= length) { // the bigint fits exactly, or must be truncated.
      System.arraycopy(intbuf, byteLen - length, dest, destPos, length);
    } else { // the bigint must be padded to fill the field
      int pad = length - byteLen;
      Arrays.fill(dest, destPos, pad + destPos, (byte) 0);
      System.arraycopy(intbuf, 0, dest, pad + destPos, byteLen);
    }
  }

  /**
   * Writes the length value to the output stream.
   *
   * @param out The Output stream to write to.
   * @param len The length to write.
   * @throws IOException If an exception occurs while writing the length.
   */
  public static void writeLen(OutputStream out, int len) throws IOException {
    out.write((byte) (len >> 8));
    out.write((byte) len);
  }

  /**
   * Adjusts the buffer to pad with zero.
   *
   * @param buffer     the buffer to pad if necessary.
   * @param byteLength The length of buffer should be
   * @return The padded buffer.
   */
  public static byte[] adjustBigBuffer(byte[] buffer, int byteLength) {
    final ByteBuffer result = ByteBuffer.allocate(byteLength);
    int skip = 0;
    //skip leading zero that BigInteger may add
    while ((buffer.length - skip) > byteLength) {
      skip++;
    }
    int pad = 0;
    // left pad with zero if not correct size
    while (buffer.length + pad < byteLength) {
      pad++;
      result.put((byte) 0);
    }
    result.put(buffer, skip, buffer.length - skip);
    result.flip();
    return unwrap(result);
  }

  /**
   * Gets the Max ServiceInfo Transmission.
   *
   * @return the max size of serviceInfo.
   */
  public static int getServiceInfoMtuSize() {
    return 1300; //from spec
  }

  /**
   * Gets the Max packet Transmission size.
   *
   * @return Max size of a packet.
   */
  public static int getPacketMtuSize() {
    return 1500; //from spec
  }


  /**
   * Gets the maximum negotiated message size.
   *
   * @return The maximum negotiated message size.
   */
  public static int getMaxBufferSize() {
    return Short.MAX_VALUE * 2 + 1;
  }
}
