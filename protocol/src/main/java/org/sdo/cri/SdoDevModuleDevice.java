// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * The SDO ServiceInfo SdoDevice Module, device-side.
 */
public final class SdoDevModuleDevice implements ServiceInfoSource {

  @Override
  public List<Entry<CharSequence, CharSequence>> getServiceInfo() {

    List<Entry<CharSequence, CharSequence>> values = new LinkedList<>();

    String value = "1";
    values.add(new SimpleEntry<>(SdoDev.KEY_ACTIVE, value));

    value = System.getProperty("os.name");
    values.add(new SimpleEntry<>(SdoDev.KEY_OS, value));

    value = System.getProperty("os.arch");
    values.add(new SimpleEntry<>(SdoDev.KEY_ARCH, value));
    values.add(new SimpleEntry<>(SdoDev.KEY_BIN, value));

    value = System.getProperty("os.version");
    values.add(new SimpleEntry<>(SdoDev.KEY_VERSION, value));

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SDO reference device, using JRE ");
    stringBuilder.append(System.getProperty("java.version"));
    stringBuilder.append(" (");
    stringBuilder.append(System.getProperty("java.vendor"));
    stringBuilder.append(")");
    value = stringBuilder.toString();
    values.add(new SimpleEntry<>(SdoDev.KEY_DEVICE, value));

    value = "0";
    values.add(new SimpleEntry<>(SdoDev.KEY_SN, value));

    value = File.separator;
    values.add(new SimpleEntry<>(SdoDev.KEY_PATHSEP, value));

    value = File.pathSeparator;
    values.add(new SimpleEntry<>(SdoDev.KEY_SEP, value));

    value = System.getProperty("line.separator");
    values.add(new SimpleEntry<>(SdoDev.KEY_NL, value));

    value = System.getProperty("java.io.tmpdir");
    values.add(new SimpleEntry<>(SdoDev.KEY_TMP, value));

    values.add(new SimpleEntry<>(SdoDev.KEY_DIR, value));

    return values;
  }
}
