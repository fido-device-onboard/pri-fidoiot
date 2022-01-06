package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum CipherSuiteType {
  A128GCM(1),
  A256GCM(3),
  AES_CCM_16_128_128(30),
  AES_CCM_16_128_256(31);

  private int id;

  CipherSuiteType(int id) {
    this.id = id;
  }

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
