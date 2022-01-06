package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;


public enum PublicKeyType  {
  RSA2048RESTR(1),
  RSAPKCS(5),
  SECP256R1(10),
  SECP384R1(11);


  private int id;

  PublicKeyType(int id) {
    this.id = id;
  }



  @JsonCreator
  public static PublicKeyType fromNumber(Number n) {
    int i = n.intValue();

    for (PublicKeyType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(PublicKeyType.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
