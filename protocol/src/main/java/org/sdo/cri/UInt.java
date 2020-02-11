// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;
import java.util.Objects;

abstract class UInt implements Serializable {

  private static final int RADIX = 10;

  // All SDO UInts can fit inside a long, and it's not clear that there are enough
  // of them in the protocol to justify the complexity of minimizing storage
  // on a case-by-case basis.
  //
  // Until there's a concrete reason for such optimization, keep UInt simple
  // and use long across the board.
  private final long value;

  UInt(long value) {
    this.value = verify(value);
  }

  UInt(CharBuffer value) {

    long accumulator = 0L;

    // unsigned decimal only, no '-', no valid prefixes
    for (int i = getDigit(value); 0 <= i; i = getDigit(value)) {
      accumulator = (accumulator * RADIX) + i;
    }

    this.value = verify(accumulator);
  }

  private static int getDigit(CharBuffer in) {

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

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }

    if (!(o instanceof UInt)) {
      return false;
    }

    UInt that = (UInt) o;
    return value == that.value;
  }

  @Override
  public String toString() {
    return Long.toString(getValue(), RADIX);
  }

  protected abstract long getMax();

  long getValue() {
    return value;
  }

  private long verify(long value) {
    if (value >= 0 && value <= getMax()) {
      return value;

    } else {
      throw new IllegalArgumentException("uint size is out of bounds");
    }
  }

  static class UInt16 extends UInt {

    UInt16(long value) {
      super(value);
    }

    UInt16(final CharBuffer value) {
      super(value);
    }

    UInt16(final UInt other) {
      super(other.getValue());
    }

    protected long getMax() {
      return (1 << 16) - 1;
    }
  }

  static class UInt32 extends UInt {

    UInt32(long value) {
      super(value);
    }

    UInt32(final CharBuffer value) {
      super(value);
    }

    UInt32(final UInt other) {
      super(other.getValue());
    }

    protected long getMax() {
      return (1L << 32) - 1;
    }
  }

  static class UInt8 extends UInt {

    UInt8(final long value) {
      super(value);
    }

    UInt8(final CharBuffer value) {
      super(value);
    }

    UInt8(final UInt other) {
      super(other.getValue());
    }

    protected long getMax() {
      return (1 << 8) - 1;
    }
  }
}
