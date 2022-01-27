// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.util.Arrays;

public class KeyExchangeResult {

  final byte[] shSe;
  final byte[] contextRand;

  KeyExchangeResult(byte[] shSe, byte[] contextRand) {
    this.shSe = Arrays.copyOf(shSe, shSe.length);
    this.contextRand = Arrays.copyOf(contextRand, contextRand.length);
  }
}


