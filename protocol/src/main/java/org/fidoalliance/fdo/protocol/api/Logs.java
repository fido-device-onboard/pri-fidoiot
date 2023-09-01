// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.fidoalliance.fdo.protocol.BufferUtils;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.DispatchMessage;
import org.fidoalliance.fdo.protocol.HttpClientSupplier;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.MessageBodyException;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;

public class Logs extends RestApi {
  protected static final LoggerService logger = new LoggerService(Logs.class);

  private File getLogFile() {
    return new File("./app-data/service.log");
  }

  @Override
  public void doGet() throws Exception {

    File file = getLogFile();
    try (BufferedReader br
        = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {

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
