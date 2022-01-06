package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum RendezvousProtocol  {
  PROT_REST(0),
  PROT_HTTP(1),
  PROT_HTTPS(2),
  PROT_TCP(3),
  PROT_TLS(4),
  PROT_COAP_TCP(5),
  PROT_COAP_UDP(6);

  private int id;

  private RendezvousProtocol(int id) {
    this.id = id;
  }

  @JsonCreator
  public static RendezvousProtocol fromNumber(Number n) {
    int i = n.intValue();

    for (RendezvousProtocol e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(
        RendezvousProtocol.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
