// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.net.URL;
import java.util.List;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.OnDieCertificateManager;



/**
 * Maintains OnDie Certificates.
 */
public class OnDie extends RestApi {
  protected static final LoggerService logger = new LoggerService(OnDie.class);

  @Override
  public void doGet() throws Exception {
    OnDieCertificateManager certManager =
            Config.getWorker(OnDieCertificateManager.class);

    List<String> certs = certManager.getCertList();
    // Set content type of the response
    getResponse().setContentType("text/plain");
    getResponse().setCharacterEncoding("UTF-8");

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
    logger.info("OnDie doPost body : " + body);

    String zipFilePathname = Mapper.INSTANCE.readValue(body, String.class);

    OnDieCertificateManager certManager =
            Config.getWorker(OnDieCertificateManager.class);

    URL zipUrl = new URL(zipFilePathname);
    certManager.loadFromZipFileUrl(zipUrl);
  }
}
