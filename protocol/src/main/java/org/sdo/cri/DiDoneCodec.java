// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DiDone}.
 */
class DiDoneCodec extends Codec<DiDone> {

  @Override
  Codec<DiDone>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<DiDone>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<DiDone>.Decoder {

    @Override
    public DiDone apply(CharBuffer in) {
      return new DiDone();
    }
  }

  private class Encoder extends Codec<DiDone>.Encoder {

    @Override
    public void apply(Writer writer, DiDone value) {
      // no body
    }
  }
}
