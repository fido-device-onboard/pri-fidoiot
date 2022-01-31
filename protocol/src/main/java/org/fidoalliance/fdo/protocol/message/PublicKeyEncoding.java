package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;


public enum PublicKeyEncoding {
  CRYPTO(0),
  X509(1),
  COSEX5CHAIN(2),
  COSEKEY(3);

  private int id;

  private PublicKeyEncoding(int id) {
    this.id = id;
  }

  @JsonCreator
  public static PublicKeyEncoding fromNumber(Number n) {
    int i = n.intValue();

    for (PublicKeyEncoding e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(PublicKeyEncoding.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}

