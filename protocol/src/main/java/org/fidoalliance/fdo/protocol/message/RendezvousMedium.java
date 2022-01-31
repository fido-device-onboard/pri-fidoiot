package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum RendezvousMedium  {
  ETH0(0),
  ETH1(1),
  ETH2(2),
  ETH3(3),
  ETH4(4),
  ETH5(5),
  ETH6(6),
  ETH7(7),
  ETH8(8),
  ETH9(9),
  ETH_ALL(20),
  WIFI0(10),
  WIFI1(11),
  WIFI2(12),
  WIFI3(13),
  WIFI4(14),
  WIFI5(15),
  WIFI6(16),
  WIFI7(17),
  WIFI8(18),
  WIFI9(19),
  WIFI_ALL(21);


  private int id;

  private RendezvousMedium(int id) {
    this.id = id;
  }

  @JsonCreator
  public static RendezvousMedium fromNumber(Number n) {
    int i = n.intValue();

    for (RendezvousMedium e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(
        RendezvousMedium.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }

}
