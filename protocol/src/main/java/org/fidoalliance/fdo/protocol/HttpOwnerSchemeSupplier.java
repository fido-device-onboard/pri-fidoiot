// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpOwnerSchemeSupplier implements OwnerSchemesSupplier {


  private static final LoggerService logger = new LoggerService(HttpOwnerSchemeSupplier.class);

  public HttpOwnerSchemeSupplier() {
    logger.warn("Owner using http protocol for TO0. Should use HTTPS in production. ");
  }

  @Override
  public List<String> get() throws IOException {
    List<String> schemes = new ArrayList<>();
    schemes.add(HttpUtils.HTTP_SCHEME);
    return schemes;
  }

}
