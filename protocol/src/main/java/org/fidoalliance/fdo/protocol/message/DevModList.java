// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.DevModListDeserializer;
import org.fidoalliance.fdo.protocol.serialization.DevModListSerializer;

@JsonDeserialize(using = DevModListDeserializer.class)
@JsonSerialize(using = DevModListSerializer.class)
public class DevModList {

  private int index;
  private int count;
  private String[] modulesNames;

  public int getIndex() {
    return index;
  }

  public int getCount() {
    return count;
  }

  public String[] getModulesNames() {
    return modulesNames;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setModulesNames(String[] modulesNames) {
    this.modulesNames = modulesNames;
  }
}
