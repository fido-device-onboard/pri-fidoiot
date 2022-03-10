// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.CertSignatureFunction;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;

public class StandardCertSignatureFunction implements CertSignatureFunction {


  @Override
  public Certificate[] apply(ManufacturingInfo info) throws IOException {

    final AnyType certInfo = info.getCertInfo();

    final Object certObject = certInfo.covertValue(Object.class);
    byte[] encoded = null;
    if (certObject instanceof byte[]) {
      encoded = (byte[]) certObject;
    } else {
      CertChain certChain = certInfo.covertValue(CertChain.class);
      return certChain.getChain().toArray(Certificate[]::new);
    }

    final JcaPKCS10CertificationRequest csr =
        new JcaPKCS10CertificationRequest(encoded);

    final KeyResolver keyResolver = Config.getWorker(ManufacturerKeySupplier.class).get();

    PrivateKey privateKey = null;
    final CryptoService cs = Config.getWorker(CryptoService.class);
    try {

      final PublicKey publicKey = csr.getPublicKey();
      final KeySizeType sizeType = new AlgorithmFinder().getKeySizeType(publicKey);
      final String alias = KeyResolver.getAlias(info.getKeyType(), sizeType);

      privateKey = keyResolver.getPrivateKey(alias);

      final Certificate[] issuerChain = keyResolver.getCertificateChain(alias);

      return new CertChainBuilder()
          .setPrivateKey(privateKey)
          .setIssuerChain(issuerChain)
          .setPublicKey(csr.getSubjectPublicKeyInfo())
          .setProvider(Config.getWorker(CryptoService.class).getProvider())
          .setSignatureAlgorithm(csr.getSignatureAlgorithm().getAlgorithm())
          .setSubject(csr.getSubject())
          .setValidityDays(Config.getWorker(ValidityDaysSupplier.class).get())
          .build();

    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    } finally {
      cs.destroyKey(privateKey);
    }


  }
}
