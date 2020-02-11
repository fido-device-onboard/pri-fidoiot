// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.security.PublicKey;

class PublicKeyCodec {

  static class Decoder implements ProtocolDecoder<PublicKey> {

    @Override
    public PublicKey decode(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);
      KeyType pkType = new KeyTypeCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      KeyEncoding pkEnc = new KeyEncodingCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      PublicKey key;

      switch (pkEnc) {

        case NONE:
          key = new PkNullCodec().decoder().apply(in);
          break;

        case X_509:
          key = new PkX509Codec().decoder().apply(in);
          break;

        case RSAMODEXP:
          key = new PkRmeCodec().decoder().apply(in);
          break;

        case EPID:
          byte[] keyBytes = new PkEpidCodec().decoder().apply(in).getEncoded();
          switch (pkType) {
            case EPIDV1_0:
              key = new EpidKey10(keyBytes);
              break;
            case EPIDV1_1:
              key = new EpidKey11(keyBytes);
              break;
            case EPIDV2_0:
              key = new EpidKey20(keyBytes);
              break;
            default:
              throw new UnsupportedOperationException(pkType.name());
          }
          break;

        default:
          throw new UnsupportedOperationException(pkEnc.toString());
      }

      Matchers.expect(in, Json.END_ARRAY);

      return key;
    }
  }

  static class Encoder implements ProtocolEncoder<PublicKey> {

    // The 'public key encoding' (*.pe fields) to be used.
    private final KeyEncoding pe;

    public Encoder(KeyEncoding pe) {
      this.pe = pe;
    }

    @Override
    public void encode(Writer writer, PublicKey value) throws IOException {

      // No matter what the requested 'pe' is, null keys can only be encoded as PkNull
      final KeyEncoding keyEncoding = null == value ? KeyEncoding.NONE : getPe();

      writer.write(Json.BEGIN_ARRAY);
      final KeyType keyType = Keys.toType(value);
      new KeyTypeCodec().encoder().apply(writer, keyType);

      writer.write(Json.COMMA);
      new KeyEncodingCodec().encoder().apply(writer, keyEncoding);

      writer.write(Json.COMMA);

      switch (keyEncoding) {
        case NONE:
          new PkNullCodec().encoder().apply(writer, null);
          break;

        case X_509:
          new PkX509Codec().encoder().apply(writer, value);
          break;

        case RSAMODEXP:
          new PkRmeCodec().encoder().apply(writer, value);
          break;

        case EPID:
          new PkEpidCodec().encoder().apply(writer, value);
          break;

        default:
          throw new UnsupportedOperationException(getPe().toString());
      }
      writer.write(Json.END_ARRAY);
    }

    KeyEncoding getPe() {
      return pe;
    }
  }
}
