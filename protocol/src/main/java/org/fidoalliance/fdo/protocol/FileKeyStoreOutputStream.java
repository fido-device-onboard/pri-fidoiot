// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreOutputStreamFunction;

public class FileKeyStoreOutputStream implements KeyStoreOutputStreamFunction {

  @Override
  public OutputStream apply(String s) throws IOException {
    return new FileOutputStream(s);
  }
}
