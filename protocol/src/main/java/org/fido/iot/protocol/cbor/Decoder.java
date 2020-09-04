// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * A Concise Binary Object Representation (CBOR) Decoder.
 */
public class Decoder implements Iterator<Object> {

  private final ReadableByteChannel myInput;
  private Object myNext;

  private Decoder(Builder builder) {
    this.myInput = builder.inputChannel;
  }

  @Override
  public boolean hasNext() {
    if (null == myNext) {
      try {
        myNext = readItem();
      } catch (NoSuchElementException e) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    } else {
      Object item = myNext;
      if (item instanceof Optional) {
        item = ((Optional<?>) item).orElse(null);
      }
      myNext = null;
      return item;
    }
  }

  private long readAdditional(int size) throws IOException {
    ByteBuffer bbuf = ByteBuffer.wrap(readByteString(size));
    switch (size) {
      case Byte.BYTES:
        return Byte.toUnsignedInt(bbuf.get());
      case Short.BYTES:
        return Short.toUnsignedInt(bbuf.getShort());
      case Integer.BYTES:
        return Integer.toUnsignedLong(bbuf.getInt());
      case Long.BYTES:
        return bbuf.getLong();
      default:
        throw new RuntimeException("BUG: unexpected size in integer read");
    }
  }

  private Object readBigNum(int signum) {
    Object o = readItem();
    if (o instanceof byte[]) {
      byte[] bytes = (byte[]) o;
      BigInteger i = new BigInteger(1, bytes);
      if (signum >= 0) {
        return i;
      } else {
        return BigInteger.valueOf(-1L).subtract(i);
      }
    } else {
      throw new IllegalArgumentException("bignums must be CBOR arrays");
    }
  }

  // cbor allows up to 64 bits for array size but java can only do 31
  private byte[] readByteString(int size) throws IOException {
    byte[] bytes = new byte[size];
    ByteBuffer bbuf = ByteBuffer.wrap(bytes);
    while (bbuf.remaining() > 0) {
      int count = myInput.read(bbuf);
      if (count < 0) { // eof, short
        throw new BufferUnderflowException();
      }
    }

    return bytes;
  }

  private Object readItem() {

    try {
      ByteBuffer header = ByteBuffer.allocate(1);
      if (myInput.read(header) < 0) {
        return null; // EOF
      }

      header.flip();
      byte b = header.get();

      // Many major types have a variable-length integer attached which describes the size
      // of the data item.
      InitialByte ib = InitialByte.of(b);
      final long val;

      switch (ib.getAi()) {
        case AdditionalInfo.LENGTH_ONE:
          val = readAdditional(Byte.BYTES);
          break;
        case AdditionalInfo.LENGTH_TWO:
          val = readAdditional(Short.BYTES);
          break;
        case AdditionalInfo.LENGTH_FOUR:
          val = readAdditional(Integer.BYTES);
          break;
        case AdditionalInfo.LENGTH_EIGHT:
          val = readAdditional(Long.BYTES);
          break;
        case 28: // additional 28 - 30 are reserved by CBOR
        case 29:
        case 30:
          throw new IllegalArgumentException("CBOR ai code is illegal: " + ib);
        case AdditionalInfo.LENGTH_INDEFINITE:
        default:
          val = ib.getAi();
      }

      switch (ib.getMt()) {
        case MajorType.UNSIGNED_INT:
          return Long.valueOf(val);

        case MajorType.NEGATIVE_INT:
          return Long.valueOf(-1L - val);

        case MajorType.BYTE_STRING:
          if (AdditionalInfo.LENGTH_INDEFINITE == val) {
            throw new UnsupportedOperationException();
          } else {
            ByteBuffer bytes = ByteBuffer.allocate((int) val);
            while (bytes.hasRemaining() && 0 <= myInput.read(bytes)) {
              ;
            }
            //change to array
            bytes.flip();
            if (bytes.hasArray()
                && bytes.array().length == bytes.remaining()
                && bytes.position() == 0) {
              return bytes.array();
            }
            //this will probably never happen but will handle
            byte[] cpy = new byte[bytes.remaining()];
            bytes.get(cpy);
            return cpy;
          }

        case MajorType.TEXT_STRING:
          if (AdditionalInfo.LENGTH_INDEFINITE == val) {
            throw new UnsupportedOperationException();
          } else {
            ByteBuffer text = ByteBuffer.allocate((int) val);
            while (text.hasRemaining() && 0 <= myInput.read(text)) {
              ;
            }
            return StandardCharsets.UTF_8.decode(text.flip()).toString();
          }

        case MajorType.ARRAY:
          if (AdditionalInfo.LENGTH_INDEFINITE == val) {
            throw new UnsupportedOperationException();
          } else {
            return LongStream.range(0, val)
                .mapToObj(l -> readItem())
                .collect(Collectors.toList());
          }

        case MajorType.MAP:
          if (AdditionalInfo.LENGTH_INDEFINITE == val) {
            throw new UnsupportedOperationException();
          } else {
            return LongStream.range(0, val)
                .mapToObj(l -> new SimpleEntry(readItem(), readItem()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          }

        case MajorType.TAG:
          switch ((int) val) {
            case Tag.POSITIVE_BIGNUM:
              return readBigNum(1);
            case Tag.NEGATIVE_BIGNUM:
              return readBigNum(-1);
            default:
              throw new IllegalArgumentException("unsupported tag: " + val);
          }

        case MajorType.SIMPLE:
          switch (ib.getAi()) {

            case AdditionalInfo.TRUE:
              return true;
            case AdditionalInfo.FALSE:
              return false;
            case AdditionalInfo.NULL:
              return Optional.empty();

            default:
              throw new IllegalArgumentException("CBOR ai code is unsupported: " + ib);
          }

        default:
          throw new IllegalArgumentException("CBOR mt code is illegal: " + ib);
      }
    } catch (IOException e) {
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public static class Builder {

    private ReadableByteChannel inputChannel;

    public Builder(ReadableByteChannel in) {
      this.inputChannel = in;
    }

    public Decoder build() {
      return new Decoder(this);
    }
  }
}
