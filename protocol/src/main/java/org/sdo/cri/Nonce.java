// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

class Nonce implements Serializable {

  private static final short BYTES = 16;
  private static final short CHARS = 24; // ceil(16 * 4 / 3), rounded up to block
  private static final String QUOTE = "\"";

  private final byte[] bytes;

  /**
   * Constructor.
   */
  public Nonce(final SecureRandom random) {
    byte[] b = new byte[BYTES];
    random.nextBytes(b);
    this.bytes = b;
  }

  /**
   * Constructor.
   */
  public Nonce(final CharBuffer cbuf) {

    // String will be CHARS b64 chars plus two quotes
    final char[] c = new char[CHARS + 2];
    cbuf.get(c);
    final String s = new String(c);

    // String must begin and end with quotes
    if (!(s.startsWith(QUOTE) || s.endsWith(QUOTE))) {
      throw new IllegalArgumentException("not a quoted-string: " + s);
    }

    this.bytes = Base64.getDecoder().decode(s.substring(1, s.length() - 1));
  }

  public byte[] getBytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Nonce nonce = (Nonce) o;
    return Arrays.equals(bytes, nonce.bytes);
  }

  @Override
  public String toString() {
    return QUOTE
        + Base64.getEncoder().encodeToString(bytes)
        + QUOTE;
  }
}
