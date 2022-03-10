// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import org.fidoalliance.fdo.protocol.serialization.CertChainDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CertChainSerializer;

@JsonSerialize(using = CertChainSerializer.class)
@JsonDeserialize(using = CertChainDeserializer.class)
public class CertChain {

  private final List<Certificate> chain;

  private CertChain(List<Certificate> chain) {
    this.chain = chain;
  }

  @JsonIgnore
  public static CertChain fromList(List<Certificate> chain) {
    return new CertChain(chain);
  }


  @JsonIgnore
  public  List<Certificate> getChain() {
    return chain;
  }

  /**
   * Gets the Cert path.
   * @return The certificate path.
   */
  @JsonIgnore
  public CertPath getPath() {
    final CertificateFactory cf;
    try {
      cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertPath(chain);
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }
}
