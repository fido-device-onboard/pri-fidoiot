// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum CipherSuiteType {
  A128GCM(1),
  A256GCM(3),
  AES_CCM_16_128_128(30),//prev spec uses 32 and 33 64
  AES_CCM_16_128_256(31),
  AES_CCM_64_128_128(32),//prev spec uses 32 and 33 64
  AES_CCM_64_128_256(33),
  COSE_AES128_CBC(-17760703),//see FDO spec Appendix E: IANA Considerations
  COSE_AES128_CTR(-17760704),
  COSE_AES256_CBC(-17760705),
  COSE_AES256_CTR(-17760706);

  private final int id;

  CipherSuiteType(int id) {
    this.id = id;
  }

  /**
   * Coverts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static CipherSuiteType fromNumber(Number n) {
    int i = n.intValue();

    for (CipherSuiteType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(CipherSuiteType.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }

}
