// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class KeyMaterialFactory {

  // All SDO key material generators use the same label text.
  private static final byte[] LABEL =
      Buffers.unwrap(StandardCharsets.US_ASCII.encode("MarshalPointKDF"));

  // All SDO key material generators use the same 'separation indicator',
  // or separator.
  private static final byte[] SEPARATOR = {0};

  // field names are given in NIST 800-108...
  private final byte[] context;
  private final int counter;
  private final String prfName;

  KeyMaterialFactory(int counter, String prfName, ByteBuffer context) {
    this.counter = counter;
    this.prfName = prfName;
    this.context = new byte[context.remaining()];
    context.get(this.context);
  }

  byte[] build() throws InvalidKeyException, NoSuchAlgorithmException {

    final Mac mac = Mac.getInstance(getPrfName(), BouncyCastleLoader.load());
    final Key zeroKey = new SecretKeySpec(new byte[]{0}, getPrfName());
    mac.init(zeroKey);

    byte encodedCounter = Integer.valueOf(getCounter()).byteValue();
    mac.update(encodedCounter);
    mac.update(getLabel());
    mac.update(getSeparator());
    mac.update(getContext());

    return mac.doFinal();
  }

  // Gets this factory's context.
  //
  // NIST 800-108 defines "context":
  //     A binary string containing the information related to the derived
  //     keying material.  It may include identities of parties who are deriving
  //     and/or using the derived keying material and, optionally, a nonce
  //     known by the parties who derive the keys.
  //
  // In SDO, the nonce is the shared secret from the key exchange.
  private ByteBuffer getContext() {
    return ByteBuffer.wrap(context).asReadOnlyBuffer();
  }

  // Gets this factory's counter, called 'i' in NIST 800-108
  //
  // NIST 800-108 defines it:
  //     A counter, a binary string of length r that is an input to each
  //     iteration of a PRF in counter mode...
  //
  // In SDO, r = 1.
  //
  // SDO also does not iterate simply, as the context changes between
  // iterations.  This class implements the inside of the loop from
  // NIST 800-108 so inputs can be changed between iterations.
  private int getCounter() {
    return counter;
  }

  // Gets this factory's label.
  //
  // NIST 800-108 defines this:
  //     A string that identifies the purpose for the derived keying material,
  //     which is encoded as a binary string.
  private ByteBuffer getLabel() {
    return ByteBuffer.wrap(LABEL).asReadOnlyBuffer();
  }

  // Gets the algorithm name of this factory's PRF.
  //
  // NIST 800-108 defines the abbreviation PRF for PseudoRandomFunction.
  // The document goes on to state that HMAC and CMAC are approved PRFs.
  // So this boils down to selecting the JCE HMAC.
  private String getPrfName() {
    return prfName;
  }

  // Gets this factory's separation indicator, or separator.
  //
  // NIST 800-108 defines this:
  //     An all zero octet. An optional data field used to indicate a
  //     separation of different variable length data fields. This indicator
  //     may be considered as a part of the encoding method for the input data
  //     and can be replaced by other indicators, for example, an indicator to
  //     represent the length of the variable length field. If, for a
  //     specific KDF, only data fields with identical length are used,
  //     then the indicator may be omitted.
  private ByteBuffer getSeparator() {
    return ByteBuffer.wrap(SEPARATOR).asReadOnlyBuffer();
  }
}
