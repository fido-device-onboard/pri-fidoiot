// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * Codec for {@link Certificate}.
 */
class CertificateCodec extends Codec<Certificate> {

  private static final String X_509 = "X.509";
  private final Codec<ByteBuffer> certBytesCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint16Codec();

  @Override
  Codec<Certificate>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<Certificate>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getCertBytesCodec() {
    return certBytesCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private class Decoder extends Codec<Certificate>.Decoder {

    @Override
    public Certificate apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);

      final int length = getLengthCodec().decoder().apply(in).intValue();

      Matchers.expect(in, Json.COMMA);
      ByteBuffer certBytes = getCertBytesCodec().decoder().apply(in);
      if (certBytes.remaining() != length) {
        throw new IOException("certBytes length mismatch");
      }
      Matchers.expect(in, Json.END_ARRAY);

      try {
        CertificateFactory certificateFactory =
            CertificateFactory.getInstance(X_509, BouncyCastleLoader.load());
        return certificateFactory.generateCertificate(
            new ByteArrayInputStream(Buffers.unwrap(certBytes)));

      } catch (CertificateException e) {
        throw new IOException(e);
      }
    }
  }

  private class Encoder extends Codec<Certificate>.Encoder {

    @Override
    public void apply(Writer writer, Certificate value) throws IOException {

      ByteBuffer der;
      try {
        der = ByteBuffer.wrap(value.getEncoded()); // X509 encodes to ASN.1 DER

      } catch (CertificateEncodingException e) {
        throw new IOException(e);
      }

      writer.write(Json.BEGIN_ARRAY);
      getLengthCodec().encoder().apply(writer, der.remaining());

      writer.write(Json.COMMA);
      getCertBytesCodec().encoder().apply(writer, der);

      writer.write(Json.END_ARRAY);
    }
  }
}
