// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CoseKeyDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CoseKeySerializer;

/**
 * CoseKey from RFC8152.
 */

@JsonSerialize(using = CoseKeySerializer.class)
@JsonDeserialize(using = CoseKeyDeserializer.class)
public class CoseKey {


  private CoseKeyCurveType crv;


  private byte[] theX;


  private byte[] theY;


  public CoseKeyCurveType getCrv() {
    return crv;
  }


  public byte[] getX() {
    return theX;
  }


  public byte[] getY() {
    return theY;
  }


  public void setCurve(CoseKeyCurveType crv) {
    this.crv = crv;
  }


  public void setX(byte[] theX) {
    this.theX = theX;
  }

  public void setY(byte[] theY) {
    this.theY = theY;
  }
}
