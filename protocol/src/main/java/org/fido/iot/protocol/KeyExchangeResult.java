package org.fido.iot.protocol;

import java.util.Arrays;

class KeyExchangeResult {

  final byte[] shSe;
  final byte[] contextRand;

  KeyExchangeResult(byte[] shSe, byte[] contextRand) {
    this.shSe = Arrays.copyOf(shSe, shSe.length);
    this.contextRand = Arrays.copyOf(contextRand, contextRand.length);
  }
}
