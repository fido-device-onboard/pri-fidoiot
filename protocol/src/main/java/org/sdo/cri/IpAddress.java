// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.CharBuffer;
import java.util.Base64;
import java.util.Objects;
import org.sdo.cri.UInt.UInt8;

/**
 * SDO composite type 'IPAddress'.
 */
class IpAddress {

  private final InetAddress address;

  /**
   * Constructor.
   */
  public IpAddress(CharBuffer cbuf) throws IOException {

    Matchers.expect(cbuf, Json.BEGIN_ARRAY);

    long len = new UInt8(cbuf).getValue();

    Matchers.expect(cbuf, Json.COMMA);
    byte[] address = Base64.getDecoder().decode(Strings.decode(cbuf));

    Matchers.expect(cbuf, Json.END_ARRAY);

    if (len != address.length) {
      throw new IOException("length mismatch");
    }

    this.address = InetAddress.getByAddress(address);
  }

  public IpAddress(InetAddress a) {
    this.address = a;
  }

  public InetAddress get() {
    return address;
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IpAddress ipAddress = (IpAddress) o;
    return Objects.equals(address, ipAddress.address);
  }

  @Override
  public String toString() {
    byte[] address = get().getAddress();

    return Json.BEGIN_ARRAY.toString()
        + new UInt8(address.length)
        + Json.COMMA
        + Strings.encode(Base64.getEncoder().encodeToString(address))
        + Json.END_ARRAY;
  }
}
