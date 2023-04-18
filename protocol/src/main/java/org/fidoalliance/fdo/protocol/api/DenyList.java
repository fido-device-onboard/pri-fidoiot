// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.entity.AllowDenyList;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;

/**
 * Deny List for RV Server.
 */
public class DenyList extends RestApi {


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
    } else {
      hashKey = UUID.fromString(getStringBody()).toString();
    }

    AllowDenyList allowList = getSession().get(AllowDenyList.class, hashKey);
    if (null != allowList) {

      allowList.setAllowed(false);
      allowList.setHash(hashKey);
      getSession().update(allowList);
    } else {
      allowList = new AllowDenyList();
      allowList.setHash(hashKey);
      allowList.setAllowed(false);
      getSession().save(allowList);
    }
    getTransaction().commit();
    logger.debug("Updated AllowList Table");
  }
}
