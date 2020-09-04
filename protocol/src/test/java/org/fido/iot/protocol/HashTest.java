// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.jupiter.api.Test;

public class HashTest {

  @Test
  void Test() throws DecoderException {

    CryptoService service = new CryptoService();

    //Setup test for HMAC256
    byte[] payload =
        StringUtils.getBytesUtf8("this is a test of SHA protocol system");

    Composite hash = service.hash(Const.SHA_256, payload);
    ByteBuffer result = hash.getAsByteBuffer(Const.HASH);

    ByteBuffer expected = ByteBuffer
        .wrap(Hex.decodeHex(
            "2904ebfa0b87ec54db3393c782fce10dc9f203bc3ae63546e7e4a0c24e3eb907"));

    assertTrue(result.compareTo(expected) == 0);

    hash = service.hash(Const.SHA_384, payload);
    result = hash.getAsByteBuffer(Const.HASH);

    expected = ByteBuffer
        .wrap(Hex.decodeHex(
            "83be86e007cc5d37e979ce2a039bdf2d6f6e4092b66faa1c07853"
                + "6c2ed252930d6408dd2982a155067bb8084690858e9"));

    assertTrue(result.compareTo(expected) == 0);

    hash = service.hash(Const.SHA_512, payload);
    result = hash.getAsByteBuffer(Const.HASH);

    expected = ByteBuffer
        .wrap(Hex.decodeHex(
            "7c923ce9c373af187a42e62c95313bc1fc93b185cabd38d4c116367e59c21b756e2133adb3"
                + "287905ba5d4d0ba17e578b1a4a8ba4b5cf3f57afcc9aedb1be8999"));

    assertTrue(result.compareTo(expected) == 0);

  }
}
