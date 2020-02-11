// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

/**
 * SDO Error.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.1.1: Error"
 */
public class Error implements ProtocolMessage {

  private ErrorCode ec;
  private String em;
  private Integer emsg;

  /**
   * Constructor.
   */
  public Error(ErrorCode ec, int emsg, String em) {
    setEc(ec);
    setEmsg(emsg);
    setEm(em);
  }

  public Error(ErrorCode ec, MessageType emsg, String em) {
    this(ec, emsg.intValue(), em);
  }

  public ErrorCode getEc() {
    return ec;
  }

  private void setEc(ErrorCode ec) {
    this.ec = ec;
  }

  public String getEm() {
    return em;
  }

  private void setEm(String em) {
    this.em = em;
  }

  public Integer getEmsg() {
    return emsg;
  }

  private void setEmsg(Integer emsg) {
    this.emsg = emsg;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.ERROR;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ec, em, emsg);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return ec == error.ec
        && Objects.equals(em, error.em)
        && Objects.equals(emsg, error.emsg);
  }

  @Override
  public String toString() {
    final StringWriter writer = new StringWriter();
    try {
      new ErrorCodec().encoder().apply(writer, this);
      return writer.toString();

    } catch (IOException e) {
      return super.toString();
    }
  }
}
