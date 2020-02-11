// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Codec for SDO "IP Address" type.
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2: Composite Types"
 */
class InetAddressCodec extends Codec<InetAddress> {

  private final Codec<ByteBuffer> dataCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint8Codec();

  @Override
  Codec<InetAddress>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<InetAddress>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getDataCodec() {
    return dataCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private class Decoder extends Codec<InetAddress>.Decoder {

    @Override
    public InetAddress apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_ARRAY);

      int len = getLengthCodec().decoder().apply(in).intValue();

      expect(in, Json.COMMA);
      ByteBuffer value = getDataCodec().decoder().apply(in);

      expect(in, Json.END_ARRAY);

      if (len != value.remaining()) {
        throw new IOException("length mismatch");
      }

      return InetAddress.getByAddress(Buffers.unwrap(value));
    }
  }

  private class Encoder extends Codec<InetAddress>.Encoder {

    @Override
    public void apply(Writer writer, InetAddress value) throws IOException {

      ByteBuffer bytes = ByteBuffer.wrap(value.getAddress());

      writer.write(Json.BEGIN_ARRAY);
      getLengthCodec().encoder().apply(writer, bytes.remaining());

      writer.append(Json.COMMA);
      getDataCodec().encoder().apply(writer, bytes);

      writer.write(Json.END_ARRAY);
    }
  }
}
