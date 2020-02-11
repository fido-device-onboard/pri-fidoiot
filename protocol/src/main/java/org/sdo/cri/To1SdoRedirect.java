// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.net.InetAddress;

class To1SdoRedirect implements ProtocolMessage {

  private String dns1;
  private InetAddress i1;
  private Integer port1;
  private HashDigest to0dh;

  /**
   * Constructor.
   */
  public To1SdoRedirect(InetAddress i1, String dns1, Integer port1, HashDigest to0dh) {
    this.i1 = i1;
    this.dns1 = dns1;
    this.port1 = port1;
    this.to0dh = to0dh;
  }

  public String getDns1() {
    return dns1;
  }

  public void setDns1(String dns1) {
    this.dns1 = dns1;
  }

  public InetAddress getI1() {
    return i1;
  }

  public void setI1(InetAddress i1) {
    this.i1 = i1;
  }

  public Integer getPort1() {
    return port1;
  }

  public void setPort1(Integer port1) {
    this.port1 = port1;
  }

  public HashDigest getTo0dh() {
    return to0dh;
  }

  public void setTo0dh(HashDigest to0dh) {
    this.to0dh = to0dh;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO1_SDO_REDIRECT;
  }
}

