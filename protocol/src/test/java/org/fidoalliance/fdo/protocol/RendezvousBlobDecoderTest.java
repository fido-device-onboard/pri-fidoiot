// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RendezvousBlobDecoderTest {

  @Test
  void getHttpDirectivesTest() throws Exception {

    Composite directive = Composite.newArray()
        .set(Const.BLOB_IP_ADDRESS, "http://127.0.0.1".getBytes())
        .set(Const.BLOB_DNS, "localhost")
        .set(Const.BLOB_PORT, 8040)
        .set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_HTTP);

    Composite directives = Composite.newArray().set(0, directive);
    Composite to1d = Composite.newArray().set(Const.TO1D_RV,directives);
    Composite rvBlob = Composite.newArray().set(Const.COSE_SIGN1_PAYLOAD, to1d.toBytes());

    List<String> res = RendezvousBlobDecoder.getHttpDirectives(rvBlob);
    assert(res.contains("http://localhost:8040"));

  }

}
