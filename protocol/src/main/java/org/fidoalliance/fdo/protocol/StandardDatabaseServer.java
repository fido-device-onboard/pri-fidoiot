// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
//import org.h2.tools.Server;


public class StandardDatabaseServer implements DatabaseServer, Closeable {


  private static final LoggerService logger = new LoggerService(DatabaseServer.class);



  @Override
  public void start() throws IOException {



  }

  @Override
  public void close() throws IOException {


  }

}
