// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

class ServiceInfoCodec extends Codec<ServiceInfo> {

  private final Codec<String> keyCodec = new StringCodec();
  private final Codec<String> valueCodec = new StringCodec();

  @Override
  Codec<ServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<ServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getValueCodec() {
    return valueCodec;
  }

  private Codec<String> getKeyCodec() {
    return keyCodec;
  }

  private class Decoder extends Codec<ServiceInfo>.Decoder {

    @Override
    public ServiceInfo apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      ServiceInfo result = new ServiceInfo();
      Codec<String>.Decoder keyDec = getKeyCodec().decoder();
      Codec<String>.Decoder valueDec = getValueCodec().decoder();

      while (true) {

        // This could be:
        // A quote (") if this is the beginning of a new key.
        // A comma (,) if we just finished reading a record and there are more to come...
        // END_OBJECT (}) if the list has finished,
        in.mark();
        char c = in.get();

        if (Json.QUOTE.equals(c)) {

          // The string codec will expect the quote, so put it back before decoding
          in.reset();
          String key = keyDec.apply(in);
          Matchers.expect(in, Json.COLON);
          String val = valueDec.apply(in);
          result.add(new SimpleEntry<>(key, val));

        } else if (Json.COMMA.equals(c)) {

          // If we've already seen one element, a comma is expected between them.
          if (result.size() < 1) {
            throw new IOException("unexpected separator");
          }

        } else if (Json.END_OBJECT.equals(c)) {

          return result;

        } else {
          throw new IOException("unexpected input: " + c);
        }
      }
    }
  }

  private class Encoder extends Codec<ServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, ServiceInfo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      Character separator = null;
      Codec<String>.Encoder keyEnc = getKeyCodec().encoder();
      Codec<String>.Encoder valueEnc = getValueCodec().encoder();

      for (Entry<CharSequence, CharSequence> entry : value) {

        if (null != separator) {
          writer.write(separator);

        } else {
          separator = Json.COMMA;
        }

        keyEnc.apply(writer, entry.getKey().toString());
        writer.write(Json.COLON);
        valueEnc.apply(writer, entry.getValue().toString());
      }

      writer.write(Json.END_OBJECT);
    }
  }
}
