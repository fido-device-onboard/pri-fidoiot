// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class MessageDispatcherTest {

  @Test
  void dispatchTest() throws Exception {

    Composite request = Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.ERROR)
        .set(Const.SM_PROTOCOL_VERSION, Const.SM_PROTOCOL_VERSION)
        .set(Const.SM_PROTOCOL_INFO, Const.SM_PROTOCOL_INFO)
        .set(Const.SM_BODY, Const.EMPTY_MESSAGE);

    MessageDispatcher obj = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return null;
      }
    };

    assertDoesNotThrow(() -> {
      obj.dispatch(request);
    });

  }

}
