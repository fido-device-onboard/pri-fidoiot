// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

import java.util.Objects;

/**
 * The Initial Byte (IB) of a CBOR data item.
 */
public class InitialByte {

  private static final int THREE_BITS = 0x07;
  private static final int FIVE_BITS = 0x1f;

  private final int myMt; // major type (mt)
  private final int myAi; // additional info (ai)

  public InitialByte(int mt, int ai) {
    myMt = mt;
    myAi = ai;
  }

  public static InitialByte of(byte b) {
    return new InitialByte((b >> 5) & THREE_BITS, b & FIVE_BITS);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InitialByte)) {
      return false;
    }
    InitialByte that = (InitialByte) o;
    return myMt == that.myMt && myAi == that.myAi;
  }

  public int getAi() {
    return myAi;
  }

  public int getMt() {
    return myMt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myMt, myAi);
  }

  public byte toByte() {
    return (byte) (((myMt & THREE_BITS) << 5) | (myAi & FIVE_BITS));
  }

  @Override
  public String toString() {
    return "InitialByte(" + myMt + ":" + myAi + ")";
  }
}

