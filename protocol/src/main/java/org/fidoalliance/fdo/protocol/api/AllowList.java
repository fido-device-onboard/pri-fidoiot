// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;

import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.entity.AllowDenyList;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;

/**
 * Allow list for RV Server.
 */
public class AllowList extends RestApi {

  protected static final LoggerService logger = new LoggerService(AllowList.class);

  @Override
  public void doPost() throws Exception {

    getTransaction();

    String hashKey = null;
    List<Certificate> certList = PemLoader.loadCerts(getStringBody());
    if (certList.size() > 0) {
      CryptoService cs = Config.getWorker(CryptoService.class);
      byte[] encoded = certList.get(0).getPublicKey().getEncoded();
      Hash hash = cs.hash(HashType.SHA384, encoded);
      hashKey = Base64.getEncoder().encodeToString(hash.getHashValue());
    }

    AllowDenyList allowList = getSession().get(AllowDenyList.class, hashKey);
    if (null != allowList) {

      allowList.setAllowed(true);
      allowList.setHash(hashKey);
      getSession().update(allowList);
    } else {
      allowList = new AllowDenyList();
      allowList.setHash(hashKey);
      allowList.setAllowed(true);
      getSession().save(allowList);
    }
    getTransaction().commit();
    logger.debug("Updated AllowList Table");
  }

  @Override
  public void doDelete() throws Exception {
    getTransaction();

    String hashKey = null;
    List<Certificate> certList = PemLoader.loadCerts(getStringBody());
    if (certList.size() > 0) {
      CryptoService cs = Config.getWorker(CryptoService.class);
      byte[] encoded = certList.get(0).getPublicKey().getEncoded();
      Hash hash = cs.hash(HashType.SHA384, encoded);
      hashKey = Base64.getEncoder().encodeToString(hash.getHashValue());
    }

    AllowDenyList allowList = getSession().get(AllowDenyList.class, hashKey);

    if (allowList != null) {
      // delete the row, if data exists.
      getSession().delete(allowList);
      logger.warn("Deleted from AllowList Table");
    } else {
      logger.warn("Certificate not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
