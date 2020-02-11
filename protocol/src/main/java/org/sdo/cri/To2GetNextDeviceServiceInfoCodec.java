// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2GetNextDeviceServiceInfoCodec extends Codec<To2GetNextDeviceServiceInfo> {

  private static final String NN = "nn";
  private static final String PSI = "psi";

  private final Codec<Number> nnCodec = new Uint32Codec();
  private final Codec<PreServiceInfo> psiCodec = new PreServiceInfoCodec();

  @Override
  Codec<To2GetNextDeviceServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2GetNextDeviceServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNnCodec() {
    return nnCodec;
  }

  private Codec<PreServiceInfo> getPsiCodec() {
    return psiCodec;
  }

  private class Decoder extends Codec<To2GetNextDeviceServiceInfo>.Decoder {

    @Override
    public To2GetNextDeviceServiceInfo apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(NN));
      final Number nn = getNnCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PSI));
      final PreServiceInfo psi = getPsiCodec().decoder().apply(in);

      expect(in, Json.END_OBJECT);

      return new To2GetNextDeviceServiceInfo(nn.intValue(), psi);
    }
  }

  private class Encoder extends Codec<To2GetNextDeviceServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, To2GetNextDeviceServiceInfo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(NN));
      getNnCodec().encoder().apply(writer, value.getNn());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PSI));
      getPsiCodec().encoder().apply(writer, value.getPsi());

      writer.write(Json.END_OBJECT);
    }
  }
}
