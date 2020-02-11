// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DeviceState}.
 */
class DeviceStateCodec extends Codec<DeviceState> {

  private final Codec<Number> numberCodec = new Uint32Codec();

  @Override
  Codec<DeviceState>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<DeviceState>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNumberCodec() {
    return numberCodec;
  }

  private class Decoder extends Codec<DeviceState>.Decoder {

    @Override
    public DeviceState apply(CharBuffer in) throws IOException {
      return DeviceState.fromNumber(getNumberCodec().decoder().apply(in));
    }
  }

  private class Encoder extends Codec<DeviceState>.Encoder {

    @Override
    public void apply(Writer writer, DeviceState value) throws IOException {
      getNumberCodec().encoder().apply(writer, value.toInteger());
    }
  }
}
