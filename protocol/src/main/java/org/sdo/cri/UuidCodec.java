// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.UUID;

class UuidCodec extends Codec<UUID> {

  private final Codec<ByteBuffer> arrayCodec = new ByteArrayCodec();

  @Override
  Codec<UUID>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<UUID>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getArrayCodec() {
    return arrayCodec;
  }

  private class Decoder extends Codec<UUID>.Decoder {

    @Override
    public UUID apply(CharBuffer in) throws IOException {

      ByteBuffer bb = getArrayCodec().decoder().apply(in);
      long hword = bb.getLong();
      long lword = bb.getLong();
      return new UUID(hword, lword);
    }
  }

  private class Encoder extends Codec<UUID>.Encoder {

    @Override
    public void apply(Writer writer, UUID value) throws IOException {

      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(value.getMostSignificantBits());
      bb.putLong(value.getLeastSignificantBits());
      bb.flip();
      getArrayCodec().encoder().apply(writer, bb);
    }
  }
}
