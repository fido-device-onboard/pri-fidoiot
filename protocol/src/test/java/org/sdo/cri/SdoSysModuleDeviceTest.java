// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.condition.OS.LINUX;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

class SdoSysModuleDeviceTest {

  @Test
  @EnabledOnOs(LINUX)
  void exec() throws Exception {
    SdoSysModuleDevice module = new SdoSysModuleDevice();
    module.exec("/bin/true\000\000");
  }

  @Test
  @EnabledOnOs(LINUX)
  void exec_predicateFails_throwsException() {
    SdoSysModuleDevice module = new SdoSysModuleDevice();

    assertThrows(RuntimeException.class, () -> {
      module.exec("/bin/false\000\000");
    });
  }

  @Test
  @EnabledOnOs(LINUX)
  void exec_timeout_throwsTimeoutException() {
    SdoSysModuleDevice module = new SdoSysModuleDevice();
    module.setExecTimeout(Duration.ofMillis(1));

    assertThrows(TimeoutException.class, () -> {
      module.exec("/bin/sleep\000999\000\000");
    });
  }
}
