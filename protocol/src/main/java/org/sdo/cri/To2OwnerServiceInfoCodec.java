// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2OwnerServiceInfoCodec extends Codec<To2OwnerServiceInfo> {

  private static String NN = "nn";
  private static String SV = "sv";

  private final Codec<Number> nnCodec = new Uint32Codec();
  private final Codec<ServiceInfo> svCodec = new ServiceInfoCodec();

  @Override
  Codec<To2OwnerServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2OwnerServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNnCodec() {
    return nnCodec;
  }

  private Codec<ServiceInfo> getSvCodec() {
    return svCodec;
  }

  private class Decoder extends Codec<To2OwnerServiceInfo>.Decoder {

    @Override
    public To2OwnerServiceInfo apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(NN));
      final Number nn = getNnCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(SV));
      final ServiceInfo sv = getSvCodec().decoder().apply(in);

      expect(in, Json.END_OBJECT);

      return new To2OwnerServiceInfo(nn.intValue(), sv);
    }
  }

  private class Encoder extends Codec<To2OwnerServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, To2OwnerServiceInfo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(NN));
      getNnCodec().encoder().apply(writer, value.getNn());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(SV));
      getSvCodec().encoder().apply(writer, value.getSv());

      writer.write(Json.END_OBJECT);
    }
  }
}
