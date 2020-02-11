// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

class PreServiceInfoCodec extends Codec<PreServiceInfo> {

  private static final String PSI_KEY_SEPARATOR = "~";
  private static final String PSI_VALUE_SEPARATOR = ",";

  private final Codec<String> stringCodec = new StringCodec();

  @Override
  Codec<PreServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<PreServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getStringCodec() {
    return stringCodec;
  }

  private class Decoder extends Codec<PreServiceInfo>.Decoder {

    @Override
    public PreServiceInfo apply(CharBuffer in) throws IOException {

      PreServiceInfo result = new PreServiceInfo();
      String encoded = getStringCodec().decoder().apply(in);

      for (String entry : encoded.split(PSI_VALUE_SEPARATOR)) {
        String[] tokens = entry.split(PSI_VALUE_SEPARATOR, 2);
        if (2 == tokens.length) {
          result.add(new SimpleEntry<>(tokens[0], tokens[1]));
        }
      }

      return result;
    }
  }

  private class Encoder extends Codec<PreServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, PreServiceInfo value) throws IOException {

      StringBuilder builder = new StringBuilder();
      String separator = null;

      for (Entry<CharSequence, CharSequence> entry : value) {

        if (null != separator) {
          builder.append(separator);

        } else {
          separator = PSI_VALUE_SEPARATOR;
        }

        builder.append(entry.getKey().toString());
        builder.append(PSI_KEY_SEPARATOR);
        builder.append(entry.getValue().toString());
      }

      getStringCodec().encoder().apply(writer, builder.toString());
    }
  }
}
