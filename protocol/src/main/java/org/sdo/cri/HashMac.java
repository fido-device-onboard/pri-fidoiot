// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * SDO "Hash" for MAC hashes.
 */
class HashMac implements Hash<MacType>, Serializable {

  private final byte[] hash;
  private final MacType type;

  HashMac() {
    this(MacType.NONE, ByteBuffer.allocate(0));
  }

  /**
   * Constructor.
   */
  HashMac(final MacType type, final ByteBuffer hash) {
    this.type = type;

    byte[] h = new byte[hash.remaining()];
    hash.get(h);
    this.hash = h;
  }

  HashMac(final MacType type, final byte[] hash) {
    this(type, ByteBuffer.wrap(hash));
  }

  private HashMac(final HashMac that) {
    this(that.getType(), that.getHash());
  }

  public HashMac(final CharBuffer cbuf) throws IOException {
    this(new HashMacCodec.HashMacDecoder().decode(cbuf));
  }

  public HashMac(byte[] mac) {
    this.type = macLengthToType(mac.length);
    this.hash = Arrays.copyOf(mac, mac.length);
  }

  private static MacType macLengthToType(int length) {
    switch (length) {
      case 256 / 8:
        return MacType.HMAC_SHA256;
      case 384 / 8:
        return MacType.HMAC_SHA384;
      default:
        throw new IllegalArgumentException("illegal MAC length: " + length);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HashMac that = (HashMac) o;
    return type == that.type && Arrays.equals(hash, that.hash);
  }

  @Override
  public ByteBuffer getHash() {
    ByteBuffer buf = ByteBuffer.allocate(hash.length);
    buf.put(hash);
    buf.flip();
    return buf;
  }

  @Override
  public MacType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(type);
    result = 31 * result + Arrays.hashCode(hash);
    return result;
  }

  @Override
  public String toString() {
    StringWriter w = new StringWriter();
    try {
      new HashMacCodec.HashMacEncoder().encode(w, this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return w.toString();
  }
}
