// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

abstract class HttpPath {

  static String of(ProtocolMessage m) {
    return of(m.getVersion(), m.getType());
  }

  static String of(Version version, MessageType type) {
    return "/mp/" + version.intValue() + "/msg/" + type.intValue();
  }
}
