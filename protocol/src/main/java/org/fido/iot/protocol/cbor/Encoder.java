// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * A Concise Binary Object Representation (CBOR) Encoder.
 */
public class Encoder {

  private final WritableByteChannel myOutput;

  private Encoder(Builder builder) {
    this.myOutput = builder.outChannel;
  }

  /**
   * Encode a java Collection as a CBOR array.
   */
  public <T> Encoder writeArray(Collection<T> val) throws IOException {
    startArray(val.size());
    for (T t : val) {
      writeObject(t);
    }
    endArray();
    return this;
  }

  /**
   * Encode a java BigInteger as a CBOR BigNum.
   */
  public Encoder writeBigNum(BigInteger val) throws IOException {
    if (0 <= val.compareTo(BigInteger.ZERO)) {
      writeTag(Tag.POSITIVE_BIGNUM);
      writeBytes(ByteBuffer.wrap(val.toByteArray()));
    } else {
      writeTag(Tag.NEGATIVE_BIGNUM);
      writeBytes(ByteBuffer.wrap(BigInteger.valueOf(-1).subtract(val).toByteArray()));
    }
    return this;
  }

  /**
   * Encode a java ByteBuffer as a CBOR byte string.
   */
  public Encoder writeBytes(ByteBuffer val) throws IOException {
    myOutput.write(header(MajorType.BYTE_STRING, val.remaining()));
    myOutput.write(val);
    return this;
  }

  /**
   * There's no use-case for really long longs ( > 63 bits) so that case is ignored. If it ever
   * becomes an issue, we can add a writeNegativeLong() method.
   */
  public Encoder writeLong(Long val) throws IOException {
    if (0 <= val) {
      myOutput.write(header(MajorType.UNSIGNED_INT, val));
    } else {
      myOutput.write(header(MajorType.NEGATIVE_INT, -1L - val));
    }
    return this;
  }

  /**
   * Encode a java Map as a CBOR map.
   */
  public <K, V> Encoder writeMap(Map<K, V> val) throws IOException {
    startMap(val.size());
    for (Map.Entry<K, V> e : val.entrySet()) {
      writeObject(e.getKey());
      writeObject(e.getValue());
    }
    endMap();
    return this;
  }

  /**
   * Encode a CBOR null.
   */
  public Encoder writeNull() throws IOException {
    myOutput.write(header(MajorType.SIMPLE, AdditionalInfo.NULL));

    return this;
  }

  /**
   * Encode a CBOR boolean.
   */
  public Encoder writeBoolean(boolean value) throws IOException {
    if (value) {
      myOutput.write(header(MajorType.SIMPLE, AdditionalInfo.TRUE));
    } else {
      myOutput.write(header(MajorType.SIMPLE, AdditionalInfo.FALSE));
    }

    return this;
  }

  /**
   * Encode a java Object as CBOR.
   */
  public Encoder writeObject(Object o) throws IOException {

    if (o instanceof Number) {
      return writeLong(((Number) o).longValue());
    } else if (o instanceof BigInteger) {
      return writeBigNum((BigInteger) o);
    } else if (o instanceof byte[]) {
      return writeBytes(ByteBuffer.wrap((byte[]) o));
    } else if (o instanceof ByteBuffer) {
      return writeBytes((ByteBuffer) o);
    } else if (o instanceof CharSequence) {
      return writeText(CharBuffer.wrap((CharSequence) o));
    } else if (o instanceof Collection) {
      return writeArray((Collection<?>) o);
    } else if (o instanceof Map) {
      return writeMap((Map<?, ?>) o);
    } else if (null == o) {
      return writeNull();
    } else if (o instanceof Boolean) {
      return writeBoolean((boolean) o);
    } else if (o instanceof Optional) {
      if (((Optional<?>) o).isEmpty()) {
        return this;
      } else {

        return writeObject(((Optional<?>) o).get());
      }
    } else {
      throw new IllegalArgumentException("unsupported type to writeObject: " + o.getClass());
    }
  }

  /**
   * Encode a CBOR tag.
   */
  public Encoder writeTag(long val) throws IOException {
    myOutput.write(header(MajorType.TAG, val));
    return this;
  }

  /**
   * Encode a java CharBuffer as a CBOR text string.
   */
  public Encoder writeText(CharBuffer val) throws IOException {
    ByteBuffer utf8 = StandardCharsets.UTF_8.encode(val);
    myOutput.write(header(MajorType.TEXT_STRING, utf8.remaining()));
    myOutput.write(utf8);
    return this;
  }

  protected Encoder endArray() {
    return this;
  }

  protected Encoder endMap() {
    return this;
  }

  protected Encoder startArray(int size) throws IOException {
    myOutput.write(header(MajorType.ARRAY, size));
    return this;
  }

  protected Encoder startMap(int size) throws IOException {
    myOutput.write(header(MajorType.MAP, size));
    return this;
  }

  ByteBuffer header(int majorType, long additional) {

    final ByteBuffer bbuf = ByteBuffer.allocate(1 + Long.BYTES);

    if (additional <= AdditionalInfo.MAXINT) {
      bbuf.put(new InitialByte(majorType, (int) additional).toByte());

    } else {

      if (additional < (1 << Byte.SIZE)) {
        bbuf.put(new InitialByte(majorType, AdditionalInfo.LENGTH_ONE).toByte());
        bbuf.put((byte) additional);

      } else if (additional < (1 << Short.SIZE)) {
        bbuf.put(new InitialByte(majorType, AdditionalInfo.LENGTH_TWO).toByte());
        bbuf.putShort((short) additional);

      } else if (additional < (1L << Integer.SIZE)) {
        bbuf.put(new InitialByte(majorType, AdditionalInfo.LENGTH_FOUR).toByte());
        bbuf.putInt((int) additional);

      } else { // there's no use-case for values > 63 bits, so ignore long overflow
        bbuf.put(new InitialByte(majorType, AdditionalInfo.LENGTH_EIGHT).toByte());
        bbuf.putLong(additional);
      }
    }

    return bbuf.flip();
  }

  public static class Builder {

    private WritableByteChannel outChannel;

    public Builder(WritableByteChannel out) {
      this.outChannel = out;
    }

    public Encoder build() {
      return new Encoder(this);
    }
  }
}
