// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Builds a signed certificate chain.
 */
public class CertChainBuilder {

  private PrivateKey privateKey;
  private Certificate[] issuerChain = new X509Certificate[0];
  private SubjectPublicKeyInfo publicKeyInfo;
  private Provider provider;
  private String signatureAlgorithm;
  private X500Name subject;
  private int validityDays;
  private GeneralNames subjectAlternateNames;


  /**
   * Sets the Private Key.
   * @param privateKey A Private Key.
   * @return The builder.
   */
  public CertChainBuilder setPrivateKey(PrivateKey privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  /**
   * Sets the issuer chain.
   * @param issuerChain The issuer chain.
   * @return The builder.
   */
  public CertChainBuilder setIssuerChain(Certificate[] issuerChain) {
    this.issuerChain = issuerChain;
    return this;
  }

  /**
   * Sets the public key.
   * @param publicKeyInfo The subject public key info.
   * @return The builder.
   */
  public CertChainBuilder setPublicKey(SubjectPublicKeyInfo publicKeyInfo) {
    this.publicKeyInfo = publicKeyInfo;
    return this;
  }

  /**
   * Sets the public key.
   * @param publicKey A Java public key.
   * @return The builder.
   */
  public CertChainBuilder setPublicKey(PublicKey publicKey) {
    this.publicKeyInfo =
        SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
    return this;
  }

  /**
   * Sets the signature algorithm.
   * @param signatureAlgorithm The algorithm as a string.
   * @return The builder.
   */
  public CertChainBuilder setSignatureAlgorithm(String signatureAlgorithm) {
    this.signatureAlgorithm = signatureAlgorithm;
    return this;
  }

  /**
   * Sets the signature algorithm.
   * @param algorithm The ANS1 signature algorithm.
   * @return The builder.
   */
  public CertChainBuilder setSignatureAlgorithm(ASN1ObjectIdentifier algorithm) {
    this.signatureAlgorithm = new DefaultAlgorithmNameFinder().getAlgorithmName(algorithm);
    return this;
  }

  /**
   * Sets the crypto provider.
   * @param provider A java cryto Provider.
   * @return The builder.
   */
  public CertChainBuilder setProvider(Provider provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Sets the Subject Name.
   * @param subject The subject as a String.
   * @return The builder.
   */
  public CertChainBuilder setSubject(String subject) {
    this.subject = new X500Name(subject);
    return this;
  }

  /**
   * Sets the subject Name.
   * @param subject The X500Name subject.
   * @return The builder.
   */
  public CertChainBuilder setSubject(X500Name subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Sets the validity days.
   * @param days the validity days.
   * @return The builder.
   */
  public CertChainBuilder setValidityDays(int days) {
    this.validityDays = days;
    return this;
  }

  /**
   * Sets the Subject Alternate Names.
   * @param names Subject Alternate Names.
   * @return The builder.
   */
  public CertChainBuilder setSubjectAlternateNames(GeneralNames names) {
    this.subjectAlternateNames = names;
    return this;
  }

  /**
   * Builds a certificate chain.
   * @return The built certificate chain.
   * @throws IOException An Error occurred.
   */
  public Certificate[] build() throws IOException {

    final Instant now = Instant.now();

    X500Name issuer = subject;
    if (issuerChain.length > 0) {
      try {
        issuer = new X509CertificateHolder(issuerChain[0].getEncoded()).getSubject();
      } catch (CertificateEncodingException e) {
        throw new IOException(e);
      }
    }

    final BigInteger serial =
        BigInteger.valueOf(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);

    final X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
        issuer,
        serial,
        Date.from(Instant.now()),
        Date.from(ZonedDateTime.now().plusDays(validityDays).toInstant()),
        subject,
        publicKeyInfo);

    if (subjectAlternateNames != null) {
      certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAlternateNames);
    }

    final JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder(
        signatureAlgorithm);

    try {
      final ContentSigner signer = jcaContentSignerBuilder.build(privateKey);
      final CertificateFactory cf = CertificateFactory.getInstance(
          "X.509", provider);

      final byte[] certBytes = certBuilder.build(signer).getEncoded();

      final X509Certificate cert =
          (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

      final Certificate[] chain = new Certificate[issuerChain.length + 1];
      chain[0] = cert;
      for (int i = 0; i < issuerChain.length; i++) {
        chain[i + 1] = issuerChain[i];
      }

      return chain;
    } catch (CertificateException | OperatorCreationException e) {
      throw new IOException(e);
    }
  }
}
