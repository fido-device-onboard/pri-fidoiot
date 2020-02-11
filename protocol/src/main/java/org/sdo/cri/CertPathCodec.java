// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec for {@link CertPath}.
 */
class CertPathCodec extends Codec<CertPath> {

  // only X509 certificates are supported, with no indication of future expansion,
  // so a single constant will suffice as a placeholder.
  private static final int TYPE_X509 = 1;
  private static final String X_509 = "X.509";
  private final Codec<Number> typeCodec = new Uint8Codec();
  private final Codec<Number> lengthCodec = new Uint8Codec();
  private final Codec<Certificate> certCodec = new CertificateCodec();

  @Override
  Codec<CertPath>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<CertPath>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Certificate> getCertCodec() {
    return certCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private Codec<Number> getTypeCodec() {
    return typeCodec;
  }

  private class Decoder extends Codec<CertPath>.Decoder {

    @Override
    public CertPath apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_ARRAY);
      final int type = getTypeCodec().decoder().apply(in).intValue();

      if (TYPE_X509 != type) {
        throw new UnsupportedOperationException("unsupported certificate type: " + type);
      }

      expect(in, Json.COMMA);
      final int length = getLengthCodec().decoder().apply(in).intValue();

      expect(in, Json.COMMA);
      expect(in, Json.BEGIN_ARRAY);
      Character separator = null;
      List<Certificate> certificates = new ArrayList<>();

      for (int i = 0; i < length; ++i) {

        if (separator != null) {
          expect(in, separator);

        } else {
          separator = Json.COMMA;
        }

        certificates.add(getCertCodec().decoder().apply(in));
      }

      expect(in, Json.END_ARRAY);
      expect(in, Json.END_ARRAY);

      try {
        CertificateFactory factory =
            CertificateFactory.getInstance(X_509, BouncyCastleLoader.load());
        return factory.generateCertPath(certificates);

      } catch (CertificateException e) {
        throw new IOException(e);
      }
    }
  }

  private class Encoder extends Codec<CertPath>.Encoder {

    @Override
    public void apply(Writer writer, CertPath value) throws IOException {

      writer.write(Json.BEGIN_ARRAY);
      getTypeCodec().encoder().apply(writer, TYPE_X509);

      writer.write(Json.COMMA);
      List<? extends Certificate> certificates = value.getCertificates();
      getLengthCodec().encoder().apply(writer, certificates.size());

      writer.write(Json.COMMA);
      writer.write(Json.BEGIN_ARRAY);
      Character separator = null;

      for (Certificate cert : certificates) {

        if (separator != null) {
          writer.append(separator);

        } else {
          separator = Json.COMMA;
        }

        getCertCodec().encoder().apply(writer, cert);
      }

      writer.write(Json.END_ARRAY);
      writer.write(Json.END_ARRAY);
    }
  }
}
