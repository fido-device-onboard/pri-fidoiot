// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.rendezvous;

import org.sdo.cri.ProtocolService;

@FunctionalInterface
interface ProtocolServiceBuilder {
  ProtocolService build();
}
