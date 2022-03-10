// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CoseProtectedHeaderDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CoseProtectedHeaderSerializer;

@JsonSerialize(using = CoseProtectedHeaderSerializer.class)
@JsonDeserialize(using = CoseProtectedHeaderDeserializer.class)
public class CoseProtectedHeader {

  private int algId;

  public int getAlgId() {
    return algId;
  }

  public void setAlgId(int algId) {
    this.algId = algId;
  }
}
