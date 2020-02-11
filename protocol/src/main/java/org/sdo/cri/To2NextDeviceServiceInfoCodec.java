// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2NextDeviceServiceInfoCodec extends Codec<To2NextDeviceServiceInfo> {

  private static String DSI = "dsi";
  private static String NN = "nn";

  private final Codec<ServiceInfo> dsiCodec = new ServiceInfoCodec();
  private final Codec<Number> nnCodec = new Uint32Codec();

  @Override
  Codec<To2NextDeviceServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2NextDeviceServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ServiceInfo> getDsiCodec() {
    return dsiCodec;
  }

  private Codec<Number> getNnCodec() {
    return nnCodec;
  }

  private class Decoder extends Codec<To2NextDeviceServiceInfo>.Decoder {

    @Override
    public To2NextDeviceServiceInfo apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(NN));
      final Number nn = getNnCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(DSI));
      final ServiceInfo dsi = getDsiCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2NextDeviceServiceInfo(nn.intValue(), dsi);
    }
  }

  private class Encoder extends Codec<To2NextDeviceServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, To2NextDeviceServiceInfo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(NN));
      getNnCodec().encoder().apply(writer, value.getNn());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(DSI));
      getDsiCodec().encoder().apply(writer, value.getDsi());

      writer.write(Json.END_OBJECT);
    }
  }
}
