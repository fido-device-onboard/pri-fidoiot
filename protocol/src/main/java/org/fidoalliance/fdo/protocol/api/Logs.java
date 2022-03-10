// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import org.fidoalliance.fdo.protocol.HttpUtils;

public class Logs extends RestApi {

  private File getLogFile() {
    return new File("./app-data/service.log");
  }

  @Override
  public void doGet() throws Exception {

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);

    File file = getLogFile();
    try (BufferedReader br
        = new BufferedReader(new FileReader(file));) {

      String line;
      while ((line = br.readLine()) != null) {
        getResponse().getOutputStream().println(line);
      }
    }
  }

  @Override
  protected void doDelete() throws Exception {
    File file = getLogFile();
    Files.write(file.toPath(), new byte[0]);
  }
}
