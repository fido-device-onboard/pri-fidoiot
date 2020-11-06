// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.fido.iot.protocol.cbor.Decoder;
import org.fido.iot.protocol.cbor.Decoder.Builder;
import org.fido.iot.protocol.cbor.Encoder;

/**
 * Provides utility methods to work with Primitive CBOR types.
 */
public class PrimitivesUtil {
  
  /**
   * Returns the CBOR null value's equivalent byte array.
   * 
   * @return byte array
   */
  public static byte[] getCborNullBytes() {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    WritableByteChannel wbc = Channels.newChannel(bao);
    Encoder encoder = new Encoder.Builder(wbc).build();
    try {
      encoder.writeNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bao.toByteArray();
  }
  
  /**
   * Compares the given object to CBOR null. Returns true if the given object is byte array or
   * {@link ByteBuffer} and is equal to CBOR null. False, otherwise.
   * 
   * @param obj The given object to be compared with
   * @return boolean value true/false
   */
  public static boolean isCborNull(Object obj) {
    ByteArrayInputStream in;
    ReadableByteChannel rbc;
    if (obj instanceof byte[]) {
      in = new ByteArrayInputStream((byte[]) obj);
    } else if (obj instanceof ByteBuffer) {
      byte[] objBytes = Composite.unwrap((ByteBuffer)obj);
      in = new ByteArrayInputStream(objBytes);
    } else {
      return false;
    }
    rbc = Channels.newChannel(in);
    Decoder decoder = new Builder(rbc).build();
    if (null == decoder.next()) {
      return true;
    }
    return false;
  }
}
