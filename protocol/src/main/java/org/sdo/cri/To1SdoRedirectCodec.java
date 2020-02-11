// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.CharBuffer;

class To1SdoRedirectCodec extends Codec<To1SdoRedirect> {

  private static final String DNS1 = "dns1";
  private static final String I1 = "i1";
  private static final InetAddress NULL_INETADDRESS = buildNullInetAddress();
  private static final String PORT1 = "port1";
  private static final String TO0DH = "to0dh";
  private final Codec<String> dns1Codec = new StringCodec();
  private final Codec<InetAddress> i1Codec = new InetAddressCodec();
  private final Codec<Number> port1Codec = new Uint32Codec();

  // The unspecified address is used as a placeholder for null or unset values.
  private static InetAddress buildNullInetAddress() {

    byte[] zeroes = {0, 0, 0, 0};

    try {
      return InetAddress.getByAddress(zeroes);

    } catch (UnknownHostException e) {
      // this can only be caused by the wrong number of bytes, and indicates
      // a fatal bug.
      throw new RuntimeException(e);
    }
  }

  @Override
  Codec<To1SdoRedirect>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To1SdoRedirect>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getDns1Codec() {
    return dns1Codec;
  }

  private Codec<InetAddress> getI1Codec() {
    return i1Codec;
  }

  private Codec<Number> getPort1Codec() {
    return port1Codec;
  }

  private class Decoder extends Codec<To1SdoRedirect>.Decoder {

    @Override
    public To1SdoRedirect apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(I1));
      InetAddress i1 = getI1Codec().decoder().apply(in);
      if (NULL_INETADDRESS.equals(i1)) {
        i1 = null;
      }

      expect(in, Json.COMMA);
      expect(in, Json.asKey(DNS1));
      String dns1 = getDns1Codec().decoder().apply(in);
      if (dns1.isEmpty()) {
        dns1 = null;
      }

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PORT1));
      final Number port1 = getPort1Codec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(TO0DH));
      final HashDigest to0dh = new HashDigest(in);

      expect(in, Json.END_OBJECT);

      return new To1SdoRedirect(i1, dns1, port1.intValue(), to0dh);
    }
  }

  private class Encoder extends Codec<To1SdoRedirect>.Encoder {

    @Override
    public void apply(Writer writer, To1SdoRedirect value) throws IOException {

      // The SDO Protocol Specification states that, in the absence of IP information,
      // 0.0.0.0 should be sent.  In the absence of DNS information, an empty string
      // should be sent.
      InetAddress i1 = value.getI1();
      if (null == i1) {
        i1 = NULL_INETADDRESS;
      }

      String dns1 = value.getDns1();
      if (null == dns1) {
        dns1 = "";
      }

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(I1));
      getI1Codec().encoder().apply(writer, i1);

      writer.write(Json.COMMA);
      writer.write(Json.asKey(DNS1));
      getDns1Codec().encoder().apply(writer, dns1);

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PORT1));
      getPort1Codec().encoder().apply(writer, value.getPort1());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(TO0DH));
      writer.write(value.getTo0dh().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
