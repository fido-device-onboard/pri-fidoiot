// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.net.InetAddress;

/**
 * An owner's location information.
 */
public class OwnerLocationInfo {

  private final InetAddress myIpAddress;
  private final String myHostname;
  private final int myPort;

  /**
   * Construct a new object.
   *
   * <p>See the SDO protocol specification 1.13a, 5.4.3, TO0.OwnerSign, for details.
   *
   * @param ipAddress The advertised IP address
   * @param hostname  The advertised hostname
   * @param port      The advertised port, or 0 for 'use default'.
   */
  public OwnerLocationInfo(InetAddress ipAddress, String hostname, int port) {
    this.myIpAddress = ipAddress;
    this.myHostname = hostname;
    this.myPort = port;
  }

  public String getHostname() {
    return myHostname;
  }

  public InetAddress getInetAddress() {
    return myIpAddress;
  }

  public int getPort() {
    return myPort;
  }
}
