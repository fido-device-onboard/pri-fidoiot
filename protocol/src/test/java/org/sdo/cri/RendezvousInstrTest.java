// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.net.InetAddress;
import java.nio.CharBuffer;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RendezvousInstrTest {

  @Test
  void copy_objectsAreEqual() {

    RendezvousInstr ri1 = new RendezvousInstr();

    ri1.setOnly(RendezvousInstr.Only.dev);
    ri1.setPo(0);

    RendezvousInstr ri2 = new RendezvousInstr(ri1);
    Assertions.assertEquals(ri1, ri2);
  }

  @Test
  void toString_fromString_objectsAreEqual() throws Exception {

    RendezvousInstr ri1 = new RendezvousInstr();

    ri1.setDelay(Duration.ofSeconds(99));
    ri1.setDn(InetAddress.getLocalHost().getCanonicalHostName());
    ri1.setIp(InetAddress.getLocalHost());
    ri1.setOnly(RendezvousInstr.Only.dev);
    ri1.setPo(100);
    ri1.setPow(200);

    RendezvousInstr ri2 = new RendezvousInstr(CharBuffer.wrap(ri1.toString()));
    Assertions.assertEquals(ri1, ri2);
  }

  @Test
  void toString_illegalCharacter_escapesChars() {

    RendezvousInstr ri1 = new RendezvousInstr();

    ri1.setOnly(RendezvousInstr.Only.dev);
    ri1.setPo(0);
    String dn = "square[brackets";
    ri1.setDn(dn);

    Assertions.assertFalse(ri1.toString().contentEquals(dn));
    Assertions.assertTrue(ri1.toString().contains("\\u005b"));
  }

  @Test
  void setDelay_negativeDelay_throwsException() {

    RendezvousInstr ri = new RendezvousInstr();
    Assertions.assertThrows(IllegalArgumentException.class, () -> ri.setDelay(Duration.ofSeconds(-1)));
  }
}
