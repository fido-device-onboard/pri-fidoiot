package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum ProtocolVersion {
  V100 (100),
  V101 (101);

  private int id;

  ProtocolVersion(int id) {
    this.id = id;
  }

  @JsonCreator
  public static ProtocolVersion fromNumber(Number n) {
    int i = n.intValue();

    for (ProtocolVersion e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(ProtocolVersion.class.getName() + ":" + i);

  }

  public static ProtocolVersion current() {
    return ProtocolVersion.V101;
  }

  public static ProtocolVersion fromString(String value) {

    switch (value) {
      case "100":
        return ProtocolVersion.V100;
      case "101":
        return ProtocolVersion.V101;
      default:
        break;
    }

    throw new NoSuchElementException(ProtocolVersion.class.getName() + ":" + value);

  }

  @JsonValue
  public int toInteger() {
    return id;
  }


  @Override
  public String toString() {
    return Integer.toString(id);
  }
}
