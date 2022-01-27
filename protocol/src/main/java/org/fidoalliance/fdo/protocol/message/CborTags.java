package org.fidoalliance.fdo.protocol.message;

import java.util.NoSuchElementException;

public enum CborTags {
  COSE_ENCRYPT_0(16),
  COSE_MAC_0(17),
  COSE_SIGN_1(18);

  private int id;

  private CborTags(int id) {
    this.id = id;
  }

  public static CborTags fromNumber(Number n) {
    int i = n.intValue();

    for (CborTags e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(CborTags.class.getName() + ":" + i);
  }

  public int toInteger() {
    return id;
  }
}
