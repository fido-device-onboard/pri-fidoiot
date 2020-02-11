// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.UUID;

class To2HelloDevice implements ProtocolMessage {

  private final CipherType cs;
  private final SigInfo ea;
  private final UUID g2;
  private final KeyExchangeType kx;
  private final Nonce n5;
  private final KeyEncoding pe;

  /**
   * Constructor.
   */
  public To2HelloDevice(
      final UUID g2,
      final Nonce n5,
      final KeyEncoding pe,
      final KeyExchangeType kx,
      final CipherType cs,
      final SigInfo ea) {

    this.g2 = g2;
    this.n5 = n5;
    this.pe = pe;
    this.kx = kx;
    this.cs = cs;
    this.ea = ea;
  }

  public CipherType getCs() {
    return cs;
  }

  public SigInfo getEa() {
    return ea;
  }

  public UUID getG2() {
    return g2;
  }

  public KeyExchangeType getKx() {
    return kx;
  }

  public Nonce getN5() {
    return n5;
  }

  public KeyEncoding getPe() {
    return pe;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_HELLO_DEVICE;
  }
}
