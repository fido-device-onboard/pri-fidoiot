// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

public interface HttpServer extends Runnable {

  String getHttpPort();

  String getHttpsPort();
}


