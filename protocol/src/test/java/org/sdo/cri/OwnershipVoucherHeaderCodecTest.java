// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.nio.CharBuffer;
import org.junit.jupiter.api.Test;

class OwnershipVoucherHeaderCodecTest {

  @Test
  void test() throws Exception {
    final String s = "{\"pv\":113,\"pe\":1,\"r\":[3,[1,{\"delaysec\":2}],[4,{\"dn\":\"10.168.152.47\",\"only\":\"owner\",\"pow\":8040,\"pr\":\"http\"}],[4,{\"dn\":\"10.168.152.47\",\"only\":\"dev\",\"po\":8040,\"pr\":\"http\"}]],\"g\":\"jbTpQYayRO2jVUWOyy0JJA==\",\"d\":\"OpenBMC\",\"pk\":[1,1,[294,\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwZ6HZE6yCEZM5w22v3RCQPBvKkGiD1PcxL74qTgf19b5WhNMgVYBCAesP3qcBOufQqV/nwW7IWjlroCgZynuNlR0LOnhGkqGukbsOwHnqygdnJEQuhEgyoVboJvWen1rBkCptXQMAgmlG8rPMrwEO9s+YzZRnrlV5Pqpd4DlRzp9i9MyrTv4foFYXB3718hZGrLGsImHGt3K+YEQ533ZYMDuy3xNktuZflnOAHzo+o/CjmkIKTNmx42PdvTGunp8+8yuNRqW7xTolF/mHrwBAhJhdG+Knh5Pwf/l+wxLdDC6PCl1aUYJfTOsgVEOtvErdELbLfZNJkiY3bHLsHy8awIDAQAB\"]],\"hdc\":[32,8,\"ImtG5p6RNmWx9nZmBIej2GeIUETvrV+/pw0/CvSc4GM=\"]}";
    OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder d = new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder();
    OwnershipVoucherHeader oh = d.decode(CharBuffer.wrap(s));

    OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder e = new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder();
    StringWriter w = new StringWriter();
    e.encode(w, oh);
    assertEquals(s, w.toString());
  }
}
