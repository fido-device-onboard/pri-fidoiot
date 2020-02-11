// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;

/**
 * Provides serialization for SDO 'Uint' types.
 *
 * <p>SDO places signing, format, and width constraint on integer types.
 */
class UintCodec extends Codec<Number> {

  private static final int RADIX = 10;

  private int width;

  UintCodec(int width) {
    this.width = width;
  }

  @Override
  Codec<Number>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<Number>.Encoder encoder() {
    return new Encoder();
  }

  private int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  class Decoder extends Codec<Number>.Decoder {

    @Override
    public Number apply(CharBuffer in) {
      long value = 0L;

      final long max = (1L << getWidth()) - 1;

      // unsigned decimal only, no '-', no valid prefixes
      for (int i = getDigit(in); 0 <= i; i = getDigit(in)) {
        value = (value * RADIX) + i;

        if (max < value) {
          throw new NumberFormatException("Uint out of bounds");
        }
      }

      return value;
    }

    private int getDigit(CharBuffer in) {

      int digit;

      try {
        in.mark();
        char c = in.get();
        digit = Character.digit(c, RADIX);

        if (digit < 0) {
          // not a digit, unread the character (usual case is normal end-of-number)
          in.reset();
        }

      } catch (BufferUnderflowException e) {
        // This is OK, it's legal to have a number be the last thing in the buffer
        digit = -1;
      }

      return digit;
    }
  }

  class Encoder extends Codec<Number>.Encoder {

    @Override
    public void apply(Writer writer, Number value) throws IOException {

      long val = value.longValue();

      if (val < 0 || (1L << getWidth()) <= val) {
        throw new NumberFormatException("Uint out of bounds");
      }
      writer.write(Long.toString(val, RADIX));
    }
  }
}
