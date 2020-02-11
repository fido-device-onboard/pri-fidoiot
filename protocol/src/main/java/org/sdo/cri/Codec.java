// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Prefer {@link ProtocolDecoder}, {@link ProtocolEncoder}.
 */
abstract class Codec<T> {

  abstract Decoder decoder();

  abstract Encoder encoder();

  abstract class Decoder {

    // Decoders and Encoders are asymmetric because decoding involves computing hashes
    // and signatures based on back-references into the original encoded version of the data.
    //
    // It would be possible to wrap such things in a fancy Reader, but this author
    // feels such a symmetric implementation would sacrifice much in clarity.
    // CharBuffers make for a clumsier design, but clearer code.
    abstract T apply(CharBuffer in) throws IOException;
  }

  abstract class Encoder {

    abstract void apply(Writer out, T value) throws IOException;
  }
}
