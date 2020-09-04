// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.util.List;

/**
 * Encodes ServiceInfo values.
 */
public class ServiceInfoEncoder {

  /**
   * Encode ServiceInfo name value pair.
   *
   * @param name  The name of ServiceInfo key.
   * @param value The value of the key.
   * @return A Composite array containing the name value pair.
   */
  public static Composite encodeValue(String name, Object value) {
    return Composite.newArray()
        .set(Const.FIRST_KEY, name)
        .set(Const.SECOND_KEY, value);
  }

  /**
   * Encodes a list of name values pairs as device serviceInfo.
   *
   * @param keyValuePairs A list of Composite name value pairs.
   * @param isMore        True if there are more device pairs to send, otherwise false.
   * @return
   */
  public static Composite encodeDeviceServiceInfo(List<Composite> keyValuePairs, boolean isMore) {
    Composite svi = Composite.newArray();
    for (Composite keyValuePair : keyValuePairs) {
      svi.set(svi.size(), keyValuePair);
    }

    return Composite.newArray()
        .set(Const.FIRST_KEY, isMore)
        .set(Const.SECOND_KEY, svi);
  }

  /**
   * Encodes a list of name values pairs as device owner serviceInfo.
   *
   * @param keyValuePairs A list of Composite name value pairs.
   * @param isMore        True if there are more owner pairs to send, otherwise false.
   * @param isDone        True if the owner is done with serviceInfo processing, otherwise false.
   * @return
   */
  public static Composite encodeOwnerServiceInfo(List<Composite> keyValuePairs, boolean isMore,
      boolean isDone) {

    Composite svi = Composite.newArray();
    for (Composite keyValuePair : keyValuePairs) {
      svi.set(svi.size(), keyValuePair);
    }

    return Composite.newArray()
        .set(Const.FIRST_KEY, isMore)
        .set(Const.SECOND_KEY, isDone)
        .set(Const.THIRD_KEY, svi);
  }
}
