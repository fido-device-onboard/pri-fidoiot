// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.jupiter.api.Test;

public class HmacTest {

  @Test
  void Test() throws DecoderException {

    CryptoService service = new CryptoService();

    //Setup test for HMAC256
    byte[] payload =
        StringUtils.getBytesUtf8("this is a test of HMAC protocol system");
    byte[] keyInfo =
        StringUtils.getBytesUtf8("This is a SHA256 key for hmac al");

    String x = Composite.toString(keyInfo);

    Composite hash = service.hash(Const.HMAC_SHA_256, keyInfo, payload);
    ByteBuffer result = hash.getAsByteBuffer(Const.HASH);

    ByteBuffer expected = ByteBuffer
        .wrap(Hex.decodeHex(
            "37e8b7174abe2c66e494bb5d9ef2fb231d2c00aeafbb265da67f0c2032f4e739"));

    assertTrue(result.compareTo(expected) == 0);

    keyInfo =
        StringUtils.getBytesUtf8("This is a SHA384 key for hmac algorithm that is ");

    hash = service.hash(Const.HMAC_SHA_384, keyInfo, payload);
    result = hash.getAsByteBuffer(Const.HASH);

    expected = ByteBuffer
        .wrap(Hex.decodeHex(
            "6e99e61e238983237f495e0485a3dfd73940ff28c346a198d500"
                + "9bae7c387fb4d1978eb78d70b30103f04851377c842b"));

    assertTrue(result.compareTo(expected) == 0);

    keyInfo =
        StringUtils.getBytesUtf8("This is a SHA512 key for hmac algorithm "
            + "that is for HMAC service");

    hash = service.hash(Const.HMAC_SHA_512_KX, keyInfo, payload);
    result = hash.getAsByteBuffer(Const.HASH);

    expected = ByteBuffer
        .wrap(Hex.decodeHex("65c31649e7c4cb4ee35910964e71dc8aa7ef528b2b4ca19f5564d53"
            + "817025bedb61fa7f2c944cb4ef6495eeebb259b601c749b488de4612be229ff681c3435af"));

    assertTrue(result.compareTo(expected) == 0);

  }
}
