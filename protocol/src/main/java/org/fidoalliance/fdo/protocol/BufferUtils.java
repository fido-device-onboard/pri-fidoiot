package org.fidoalliance.fdo.protocol;

import java.nio.ByteBuffer;

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
   * Gets the maximum negotiated message size
   * @return The maximum negotiated message size.
   */
  public static int getMaxBufferSize() {
    return Short.MAX_VALUE * 2 +1;
  }
}
