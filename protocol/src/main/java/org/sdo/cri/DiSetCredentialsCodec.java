// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DiSetCredentials}.
 */
abstract class DiSetCredentialsCodec {

  private static final String OH = "oh";

  static class DiSetCredentialsDecoder implements ProtocolDecoder<DiSetCredentials> {

    private final OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder ohDec =
        new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder();
    private char[] lastOh = new char[0];

    private void setLastOh(final CharBuffer cbuf) {
      lastOh = new char[cbuf.remaining()];
      cbuf.get(lastOh);
    }

    /**
     * Return the 'oh' text last read by this codec.
     *
     * <p>This is needed for generating hashes after parsing.
     */
    public CharBuffer getLastOh() {
      CharBuffer cbuf = CharBuffer.allocate(lastOh.length);
      cbuf.put(lastOh);
      cbuf.flip();
      return cbuf;
    }

    @Override
    public DiSetCredentials decode(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(OH));
      CharBuffer ohBuf = in.asReadOnlyBuffer();
      final OwnershipVoucherHeader oh = ohDec.decode(in);
      ohBuf.limit(in.position());
      setLastOh(ohBuf);

      expect(in, Json.END_OBJECT);

      return new DiSetCredentials(oh);
    }

    public CharBuffer getLastPk() {
      return ohDec.getLastPk();
    }
  }

  static class DiSetCredentialsEncoder implements ProtocolEncoder<DiSetCredentials> {

    @Override
    public void encode(Writer writer, DiSetCredentials value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(OH));
      new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder().encode(writer, value.getOh());

      writer.write(Json.END_OBJECT);
    }
  }
}
