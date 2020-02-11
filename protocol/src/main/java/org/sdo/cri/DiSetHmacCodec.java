// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DiSetHmac}.
 */
class DiSetHmacCodec extends Codec<DiSetHmac> {

  private static final String HMAC = "hmac";

  @Override
  Codec<DiSetHmac>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<DiSetHmac>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<DiSetHmac>.Decoder {

    @Override
    public DiSetHmac apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(HMAC));
      HashMac hmac = new HashMac(in);

      expect(in, Json.END_OBJECT);

      return new DiSetHmac(hmac);
    }
  }

  private class Encoder extends Codec<DiSetHmac>.Encoder {

    @Override
    public void apply(Writer writer, DiSetHmac value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(HMAC));
      writer.write(value.getHmac().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
