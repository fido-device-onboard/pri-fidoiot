// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.OnDieCertificateManager;

import java.net.URL;
import java.util.List;

public class OnDie extends RestApi {

  @Override
  public void doGet() throws Exception {
    OnDieCertificateManager certManager =
            Config.getWorker(OnDieCertificateManager.class);

    List<String> certs = certManager.getCertList();
    getResponse().setContentType("text/plain");  // Set content type of the response so that jQuery knows what it can expect.
    getResponse().setCharacterEncoding("UTF-8"); // You want world domination, huh?

    getResponse().getWriter().write("\n");
    getResponse().getWriter().write("Number of certs/crl: " + certs.size());
    for (String id : certs) {
      getResponse().getWriter().write("\n" + id);
    }
    getResponse().getWriter().write("\n");
  }

  @Override
  public void doPost() throws Exception {

    String body = getStringBody();

    String zipFilePathname = Mapper.INSTANCE.readValue(body, String.class);

    OnDieCertificateManager certManager =
            Config.getWorker(OnDieCertificateManager.class);

    URL zipUrl = new URL(zipFilePathname);
    certManager.LoadFromZipFileURL(zipUrl);
  }
}
