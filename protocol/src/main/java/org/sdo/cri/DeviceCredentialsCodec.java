// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DeviceCredentials113}.
 */
class DeviceCredentialsCodec extends Codec<DeviceCredentials113> {

  private static final String M = "M";
  private static final String O = "O";
  private static final String SECRET = "Secret";
  private static final String ST = "ST";

  private final Codec<ManufacturerBlock> mbCodec = new ManufacturerBlockCodec();
  private final Codec<OwnerBlock> obCodec = new OwnerBlockCodec();
  private final Codec<ByteBuffer> secretCodec = new ByteArrayCodec();
  private final Codec<DeviceState> stCodec = new DeviceStateCodec();

  @Override
  Codec<DeviceCredentials113>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<DeviceCredentials113>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ManufacturerBlock> getMCodec() {
    return mbCodec;
  }

  private Codec<OwnerBlock> getOCodec() {
    return obCodec;
  }

  private Codec<ByteBuffer> getSecretCodec() {
    return secretCodec;
  }

  private Codec<DeviceState> getStCodec() {
    return stCodec;
  }

  private class Decoder extends Codec<DeviceCredentials113>.Decoder {

    @Override
    public DeviceCredentials113 apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);
      Matchers.expect(in, Json.asKey(ST));
      final DeviceState st = getStCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(SECRET));
      final ByteBuffer secret = getSecretCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(M));
      final ManufacturerBlock m = getMCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(O));
      final OwnerBlock o = getOCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new DeviceCredentials113(st, Buffers.unwrap(secret), m, o);
    }
  }

  private class Encoder extends Codec<DeviceCredentials113>.Encoder {

    @Override
    public void apply(Writer writer, DeviceCredentials113 value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(ST));
      getStCodec().encoder().apply(writer, value.getSt());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(SECRET));
      getSecretCodec().encoder().apply(writer, ByteBuffer.wrap(value.getSecret()));

      writer.write(Json.COMMA);
      writer.write(Json.asKey(M));
      getMCodec().encoder().apply(writer, value.getM());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(O));
      getOCodec().encoder().apply(writer, value.getO());

      writer.write(Json.END_OBJECT);
    }
  }
}
