// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;

class UriCodec extends Codec<URI> {

  private final Codec<String> stringCodec = new StringCodec();

  @Override
  Codec<URI>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<URI>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getStringCodec() {
    return stringCodec;
  }

  private class Decoder extends Codec<URI>.Decoder {

    @Override
    public URI apply(CharBuffer in) throws IOException {
      String s = getStringCodec().decoder().apply(in);

      try {
        return new URI(s);

      } catch (URISyntaxException e) {
        throw new IOException(e);
      }
    }
  }

  private class Encoder extends Codec<URI>.Encoder {

    @Override
    public void apply(Writer writer, URI value) throws IOException {
      getStringCodec().encoder().apply(writer, value.toString());
    }
  }
}
