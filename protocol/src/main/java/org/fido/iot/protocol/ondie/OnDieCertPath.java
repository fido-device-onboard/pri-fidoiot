// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.ondie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fido.iot.protocol.Const;


public class OnDieCertPath {

  private CertificateFactory certFactory;

  /**
   * Constructor.
   *
   */
  public OnDieCertPath() throws CertificateException {
    certFactory = CertificateFactory.getInstance(Const.X509_ALG_NAME,  new BouncyCastleProvider());
  }

  /**
   * Builds a cert path for the OnDie device.
   *
   * @param deviceCertChain the cert chain from ROM to leaf from the device
   * @return a complete CA to leaf cert path
   * @throws Exception if error
   */
  public CertPath buildCertPath(
      byte[] deviceCertChain, OnDieCache onDieCache)
      throws CertificateException, IOException, IllegalArgumentException {

    List<Certificate> deviceCertChainList = new ArrayList<Certificate>();

    // parse the device input chain into a list of x509 certificates
    List<byte[]> certList = deserializeCertificateChain(deviceCertChain);
    for (byte[] array : certList) {
      InputStream is = new ByteArrayInputStream(array);
      deviceCertChainList.add(certFactory.generateCertificate(is));
    }

    // Based on the ROM certificate of the device, look up the issuing certificate
    // and then each subsequent issuing certificate. Append them to the list
    // to complete an end-to-end cert chain.
    // leaf -> DAL -> Kernel -> ROM -> platform -> intermediate -> OnDie CA

    // Start with the ROM cert, loop until we get to the CA
    String certId  = getIssuingCertificate(deviceCertChainList.get(0));
    while (true) {
      byte[] cert = onDieCache.getCertOrCrl(certId);
      if (cert == null) {
        throw new IllegalArgumentException(
          "Could not find cert in OnDie cache: unable to build cert chain for device: " + certId);
      }
      ByteArrayInputStream is = new ByteArrayInputStream(cert);
      X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(is);
      deviceCertChainList.add(x509Cert);
      is.close();

      // if we are at the root CA then we have the complete chain
      try {
        certId = getIssuingCertificate(x509Cert);
      } catch (Exception ex) {
        // if we never get to a ROOT CA this means the chain is malformed
        // so exit the loop and use what we have. Signature verification will
        // fail later when validating the cert chain. Don't fail now since we
        // don't want one bad chain to shutdown the entire service.
        break;
      }
    }

    return certFactory.generateCertPath(deviceCertChainList);
  }

  private String getIssuingCertificate(Certificate cert)
      throws IllegalArgumentException, CertificateEncodingException, IOException {
    X509CertificateHolder certholder = new X509CertificateHolder(cert.getEncoded());
    AuthorityInformationAccess aia =
        AuthorityInformationAccess.fromExtensions(certholder.getExtensions());
    if (aia == null) {
      throw new IllegalArgumentException(
        "AuthorityInformationAccess Extension missing from device certificate.");
    }
    AccessDescription[] descs = aia.getAccessDescriptions();
    if (descs.length != 1) {
      throw new IllegalArgumentException(
        "Too many descriptions in AIA certificate extension: " + descs.length);
    }
    return descs[0].getAccessLocation().getName().toString();
  }

  /**
   * Deserializes a certificate chain byte array and returns a more flexible data-structure
   * representing the chain.
   * Given chain should be of the following format:
   * [ number of certificates (2 bytes) | size of each certificate (2 bytes each) |
   * certificate chain from root to leaf ].
   *
   * @param certChain The certificate chain to parse
   * @return a list of deserialized certificates
   * @throws IllegalArgumentException if error
   */
  private List<byte[]> deserializeCertificateChain(byte[] certChain)
      throws IllegalArgumentException {
    List<byte[]> deserializedCertChain = new ArrayList<byte[]>();
    int offset = 0;

    // get number of certificates
    int numOfCerts;
    numOfCerts = getShortFromBytes(certChain, offset);
    offset += 2;

    // get sizes of all certificates
    int[] sizesOfCertificates = new int[numOfCerts];

    for (int i = 0; i < numOfCerts; i++) {
      sizesOfCertificates[i] = getShortFromBytes(certChain, offset);
      offset += 2;
    }

    // deserialize certificate chain
    for (int i = 0; i < numOfCerts; i++) {
      byte[] cert = Arrays.copyOfRange(certChain, offset, offset + sizesOfCertificates[i]);
      offset += cert.length;
      deserializedCertChain.add(cert);
    }

    // check that we reached the end of the chain
    if (offset < certChain.length) {
      throw new IllegalArgumentException("Certificate chain is larger than expected.");
    }
    return deserializedCertChain;
  }

  /**
   * Gets a short from a byte array (via out parameter), and returns the amount of bytes
   * read by this method.
   *
   * @param bytes The byte array from which to parse the short.
   * @param startIndex The starting index of the short.
   * @return the resulting short value
   * @throws IllegalArgumentException if error
   */
  private short getShortFromBytes(byte[] bytes, int startIndex) throws IllegalArgumentException {
    if (bytes.length < startIndex + 2) {
      throw new IllegalArgumentException("Byte array size is too small, or startIndex is too big.");
    }

    byte[] shortArr = Arrays.copyOfRange(bytes, startIndex, startIndex + 2);

    // firmware returns the value in big-endian
    ByteBuffer wrapped = ByteBuffer.wrap(shortArr); // big-endian by default
    return wrapped.getShort();
  }

}

