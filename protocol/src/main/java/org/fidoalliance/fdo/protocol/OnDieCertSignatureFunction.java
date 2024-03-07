// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.fidoalliance.fdo.protocol.db.OnDieCertificateManager;
import org.fidoalliance.fdo.protocol.dispatch.CertSignatureFunction;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;

public class OnDieCertSignatureFunction implements CertSignatureFunction {

  private final CertificateFactory certFactory;
  private static final LoggerService logger = new LoggerService(OnDieCertSignatureFunction.class);

  /**
   * Constructor.
   */
  public OnDieCertSignatureFunction() throws IOException {
    try {
      certFactory = CertificateFactory.getInstance(
          "X.509", // TODO Const.X509_ALG_NAME,
          new BouncyCastleFipsProvider());
    } catch (CertificateException e) {
      throw new IOException(e);
    }
  }

  /**
   * Used to determine if a voucher is for on OnDie device by checking if certificate list is OnDie
   * or not.
   *
   * @param certificateList list of certificates from voucher
   * @return true if certificate list is OnDie
   */
  public boolean isOnDieCertificateChain(Certificate[] certificateList)
      throws IOException, CertificateEncodingException {
    try {
      String certId = getIssuingCertificate(certificateList[0]);
      OnDieCertificateManager certManager =
          Config.getWorker(OnDieCertificateManager.class);
      if (certManager.isOnDieRootCA(certId)) {
        return true;
      }
    } catch (CertificateEncodingException ex) {
      logger.error("Invalid certificate encoding.");
    }
    return false;
  }

  @Override
  public Certificate[] apply(ManufacturingInfo info) throws IOException {

    List<Certificate> deviceCertChainList = null;

    try {
      OnDieCertificateManager certManager =
          Config.getWorker(OnDieCertificateManager.class);

      /**
       * Builds a cert path for the OnDie device.
       *
       * @param deviceCertChain the cert chain from ROM to leaf from the device
       * @return a complete CA to leaf cert path
       * @throws Exception if error
       */

      List<Certificate> romToLeafCertList = new ArrayList<Certificate>();

      // parse the device input chain into a list of x509 certificates
      List<byte[]> certList = deserializeCertificateChain(info.getOnDieDeviceCertChain());
      for (byte[] array : certList) {
        InputStream is = new ByteArrayInputStream(array);
        romToLeafCertList.add(certFactory.generateCertificate(is));
      }

      // Based on the ROM certificate of the device, look up the issuing certificate
      // and then each subsequent issuing certificate. Append them to the list
      // to complete an end-to-end cert chain.
      // leaf -> DAL -> Kernel -> ROM -> platform -> intermediate -> OnDie CA

      // Start with the ROM cert, loop until we get to the CA
      String certId = getIssuingCertificate(
          romToLeafCertList.get(0));
      List<Certificate> platformToCaList = new ArrayList<Certificate>();
      while (true) {

        // read OnDie cert from database
        byte[] cert = certManager.getCertificate(certId);

        if (cert == null) {
          throw new IllegalArgumentException(
              "Could not find cert in OnDie cache: unable to build cert chain for device: "
                  + certId);
        }
        ByteArrayInputStream is = new ByteArrayInputStream(cert);
        X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(is);
        platformToCaList.add(x509Cert);
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

      final Set<TrustAnchor> anchors = new HashSet<>();

      // put the list in order from CA -> ... ROM -> ... leaf
      deviceCertChainList = new ArrayList<Certificate>();
      for (int i = romToLeafCertList.size() - 1; i >= 0; i--) {
        deviceCertChainList.add(romToLeafCertList.get(i));
        if (i != 0) {
          anchors.add(new TrustAnchor((X509Certificate) romToLeafCertList.get(i), null));
        }
      }
      for (int i = 0; i < platformToCaList.size(); i++) {
        deviceCertChainList.add(platformToCaList.get(i));
        anchors.add(new TrustAnchor((X509Certificate) platformToCaList.get(i), null));
      }

      // validate certpath before returning
      CertPath cp = certFactory.generateCertPath(deviceCertChainList);

      final CertPathValidator validator =
          CertPathValidator.getInstance(StandardCryptoService.VALIDATOR_ALG_NAME);
      final PKIXParameters params = new PKIXParameters(anchors);
      // revocations handled differently for OnDie so do elsewhere
      // for now, just validate the cert chain itself
      params.setRevocationEnabled(false);
      params.setTrustAnchors(anchors);
      validator.validate(cp, params);
    } catch (NoSuchAlgorithmException
        | InvalidAlgorithmParameterException
        | CertificateException
        | CertPathValidatorException e) {
      throw new IOException(e);
    } finally {
      //don't do anything
    }

    Certificate[] certArray = new Certificate[deviceCertChainList.size()];
    for (int i = 0; i < deviceCertChainList.size(); i++) {
      certArray[i] = deviceCertChainList.get(i);
    }
    return certArray;
  }


  private String getIssuingCertificate(Certificate cert)
      throws IllegalArgumentException, IOException, CertificateEncodingException {
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
   * representing the chain. Given chain should be of the following format: [ number of certificates
   * (2 bytes) | size of each certificate (2 bytes each) | certificate chain from root to leaf ].
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
   * Gets a short from a byte array (via out parameter), and returns the amount of bytes read by
   * this method.
   *
   * @param bytes      The byte array from which to parse the short.
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

  /**
   * Checks for revocations.
   *
   * @param certificateList list of certificates containing revocations
   * @return true if revocation check failed
   */
  public boolean checkRevocations(Certificate[] certificateList) {
    try {
      OnDieCertificateManager certManager =
          Config.getWorker(OnDieCertificateManager.class);

      CertificateFactory certificateFactory =
          CertificateFactory.getInstance(StandardCryptoService.X509_ALG_NAME);
      for (Certificate cert : certificateList) {
        X509Certificate certificate = (X509Certificate) cert;
        byte[] crlDistributionPointDerEncodedArray = certificate.getExtensionValue(
                Extension.cRLDistributionPoints.getId());

        if (crlDistributionPointDerEncodedArray != null) {
          ASN1InputStream oasnInstream = new ASN1InputStream(
                  new ByteArrayInputStream(crlDistributionPointDerEncodedArray));
          ASN1Primitive derObjCrlDP = oasnInstream.readObject();
          DEROctetString dosCrlDP = (DEROctetString) derObjCrlDP;


          byte[] crldpExtOctets = dosCrlDP.getOctets();
          ASN1InputStream mainStream = new ASN1InputStream(
                  new ByteArrayInputStream(crldpExtOctets));
          ASN1Primitive derObj2 = mainStream.readObject();
          CRLDistPoint distPoints = CRLDistPoint.getInstance(derObj2);

          if (distPoints != null) {
            for (DistributionPoint dp : distPoints.getDistributionPoints()) {
              GeneralName[] generalNames =
                      GeneralNames.getInstance(dp.getDistributionPoint().getName()).getNames();
              for (GeneralName generalName : generalNames) {
                String name = generalName.toString();
                byte[] crlBytes = certManager.getCertificate(name.substring(name.indexOf("http")));
                if (crlBytes == null) {
                  // TODO logger.info("CRL: " + generalName.getName().toString()
                  //        + " not found in cache for cert: "
                  //        + x509cert.getIssuerX500Principal().getName());
                  return false;
                } else {
                  CRL crl = certificateFactory.generateCRL(new ByteArrayInputStream(crlBytes));
                  if (crl.isRevoked(cert)) {
                    return false;
                  }
                }
              }
            }
          }
        }
      }
    } catch (IOException | CertificateException | CRLException ex) {
      return false;
    }
    return true;
  }

}
