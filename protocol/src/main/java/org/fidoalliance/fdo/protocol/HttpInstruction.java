// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

/**
 * HttpInstruction class.
 */
public class HttpInstruction {

  private String address;
  private long delay;
  private boolean rendezvousBypass;

  /**
   * Gets the http address.
   *
   * @return the http url as a string.
   */
  public String getAddress() {
    return address;
  }

  /**
   * The delay between retry attempts.
   *
   * @return The delay in seconds.
   */
  public long getDelay() {
    return delay;
  }

  /**
   * Gets the Rendezvous bypass flag.
   *
   * @return True if TO1 is to be bypassed.
   */
  public boolean isRendezvousBypass() {
    return rendezvousBypass;
  }

  /**
   * Sets the http url string.
   *
   * @param address a URL String.
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Sets the delay seconds.
   *
   * @param delay The delay seconds.
   */
  public void setDelay(long delay) {
    this.delay = delay;
  }

  /**
   * Sets Rendezvous bypass mode.
   *
   * @param rendezvousBypass True if the TO1 is to be bypassed.
   */
  public void setRendezvousBypass(boolean rendezvousBypass) {
    this.rendezvousBypass = rendezvousBypass;
  }
}
