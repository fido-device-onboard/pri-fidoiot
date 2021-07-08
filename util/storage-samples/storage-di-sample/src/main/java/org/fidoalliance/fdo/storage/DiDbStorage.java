// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.CloseableKey;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DiServerStorage;
import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.InvalidJwtException;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;
import org.fidoalliance.fdo.protocol.ondie.OnDieCertPath;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;

/**
 * Device Initialization Database Storage.
 */
public class DiDbStorage implements DiServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final CertificateResolver resolver;
  private final OnDieService onDieService;
  private Composite voucher;
  private static final LoggerService logger = new LoggerService(DiDbStorage.class);

  /**
   * Constructs a DiDbStorage instance.
   *
   * @param cryptoService The cryptoService for signature operations.
   * @param dataSource    The SQL datasource.
   * @param resolver      The Certificate resolver.
   * @param onDieService  Service for OnDie functions.
   */
  public DiDbStorage(CryptoService cryptoService,
      DataSource dataSource,
      CertificateResolver resolver,
      OnDieService onDieService) {
    this.cryptoService = cryptoService;
    this.dataSource = dataSource;
    this.resolver = resolver;
    this.onDieService = onDieService;
  }

  @Override
  public Composite createVoucher(Object createParams) {

    if (createParams == Optional.empty() || createParams == null
        || !(createParams instanceof List)) {
      throw new InvalidMessageException("mino must be an array");
    }

    // Determine type of device and create appropriate voucher
    // The M string (mstr) is a CBOR array of several values:
    // FIRST_KEY (number): the key type requirement of the device, as an integer-coded KeyType
    //   This is the type of owner key the device is prepared to parse, not the type
    //   of the device's key.
    // SECOND_KEY (string): the device serial number
    // THIRD_KEY (string): a device info hint
    // FOURTH_KEY (bstr):
    // - if the device is using an EC keypair, a CSR.
    // - if the device is OnDie ECDSA then the device cert chain and a
    //   test signature is also present
    // FIFTH_KEY (bstr): present only for OnDie ECDSA: the test signature

    Composite mstr = Composite.fromObject(createParams);

    voucher = Composite.newArray();
    Composite header = Composite.newArray();
    String customerId = null;

    Composite settings = getSettings();
    Integer validityDays = settings.getAsNumber(Const.FIRST_KEY).intValue();
    if (settings.get(Const.THIRD_KEY) != null
        && settings.get(Const.THIRD_KEY) != Optional.empty()) {
      customerId = settings.getAsString(Const.THIRD_KEY);
    }

    //setup cert hash and public key where applicable
    Object publicKey = null;
    Composite chainHash = null;
    Composite chain = null;

    UUID guid = UUID.randomUUID();

    int keyType = mstr.getAsNumber(Const.FIRST_KEY).intValue();
    String serialNo = mstr.getAsString(Const.SECOND_KEY);

    if (keyType == Const.PK_SECP256R1 || keyType == Const.PK_SECP384R1) {
      Certificate[] issuerChain = resolver.getCertChain(keyType);

      if (mstr.get(Const.FOURTH_KEY) != Optional.empty()) {
        chain = createChain(
                mstr.getAsBytes(Const.FOURTH_KEY),
                issuerChain,
                validityDays);
        int hashType = getCryptoService().getCompatibleHashType(issuerChain[0].getPublicKey());
        chainHash = cryptoService.hash(hashType, chain.toBytes());
      }

      publicKey = cryptoService.encode(issuerChain[0].getPublicKey(),
              cryptoService.getCompatibleEncoding(issuerChain[0].getPublicKey()));
    } else if (keyType == Const.PK_RSA2048RESTR) {
      // EPID type device
      Certificate[] issuerChain = resolver.getCertChain(keyType);
      publicKey = cryptoService.encode(issuerChain[0].getPublicKey(),
             Const.PK_ENC_CRYPTO);
      chain = Composite.newArray();
      int hashType = getCryptoService().getCompatibleHashType(issuerChain[0].getPublicKey());
      chainHash = cryptoService.hash(hashType, chain.toBytes());
    } else if (keyType == Const.PK_ONDIE_ECDSA_384) {
      // Ondie ECDSA type device
      // build the cert chain and hash for the OnDie ECDSA device
      try {
        OnDieCertPath onDieCertPath = new OnDieCertPath();

        final CertPath certPath =
                onDieCertPath.buildCertPath(mstr.getAsBytes(Const.FOURTH_KEY),
                        this.onDieService.getOnDieCache());

        // validate test signature against certpath
        if (!onDieService.validateSignature(
                (List<Certificate>) certPath.getCertificates(),
                serialNo.getBytes(),
                mstr.getAsBytes(Const.FIFTH_KEY))) {
          throw new InvalidMessageException("OnDie test signature failure.");
        }

        chain = Composite.newArray();
        for (Certificate cert : certPath.getCertificates()) {
          chain.set(chain.size(), cert.getEncoded());
        }

        int hashType = getCryptoService().getCompatibleHashType(
                certPath.getCertificates().get(0).getPublicKey());

        chainHash = cryptoService.hash(hashType, chain.toBytes());

        publicKey = cryptoService.encode(certPath.getCertificates().get(0).getPublicKey(),
                cryptoService.getCompatibleEncoding(
                        certPath.getCertificates().get(0).getPublicKey()));
      } catch (Exception ex) {
        throw new InvalidMessageException(ex.getMessage());
      }
    } else {
      // placeholder for future device types
      throw new InvalidMessageException("Unsupported keytype in mstr: " + keyType);
    }

    String modelNo = mstr.getAsString(Const.THIRD_KEY);
    Composite rvInfo = Composite.fromObject(settings.getAsString(Const.SECOND_KEY));

    header.set(Const.OVH_VERSION, Const.PROTOCOL_VERSION_100);
    header.set(Const.OVH_GUID, guid);
    header.set(Const.OVH_RENDEZVOUS_INFO, rvInfo);
    header.set(Const.OVH_DEVICE_INFO, modelNo);
    header.set(Const.OVH_PUB_KEY, publicKey);
    header.set(Const.OVH_CERT_CHAIN_HASH, chainHash);

    voucher.set(Const.OV_HEADER, header);
    voucher.set(Const.OV_HMAC, Composite.newArray());
    voucher.set(Const.OV_DEV_CERT_CHAIN, chain);
    voucher.set(Const.OV_ENTRIES, Composite.newArray());

    String sql = "MERGE INTO MT_DEVICES ("
        + "GUID,"
        + "SERIAL_NO,"
        + "VOUCHER,"
        + "CUSTOMER_ID,"
        + "M_STRING,"
        + "STARTED) VALUES (?,?,?,?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      pstmt.setString(2, serialNo);
      pstmt.setBytes(3, voucher.toBytes());
      pstmt.setString(4, customerId);
      pstmt.setBytes(5, mstr.toBytes());
      Timestamp createdAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(6, createdAt);
      pstmt.execute();

    } catch (SQLException e) {
      throw new DispatchException(e);
    }

    return voucher;
  }

  @Override
  public Composite getVoucher() {
    return voucher;
  }

  @Override
  public void storeVoucher(Composite voucher) {

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);

    String sql = "UPDATE MT_DEVICES "
        + "SET VOUCHER = ? ,"
        + "COMPLETED = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, voucher.toBytes());
      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(2, updatedAt);
      pstmt.setString(3, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    logger.info("Voucher stored with GUID: " + guid.toString());
  }

  @Override
  public void starting(Composite request, Composite reply) {

  }

  @Override
  public void started(Composite request, Composite reply) {

    Composite body = reply.getAsComposite(Const.SM_BODY);
    Composite ovh = body.getAsComposite(Const.FIRST_KEY);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);
    reply.set(Const.SM_PROTOCOL_INFO,
        Composite.newMap().set(Const.PI_TOKEN, guid.toString()));
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    String token = getToken(request);

    String sql = "SELECT VOUCHER FROM MT_DEVICES "
        + "WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, token);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          voucher = Composite.fromObject(rs.getBytes(1));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    if (voucher == null) {
      throw new InvalidJwtException();
    }

  }

  @Override
  public void continued(Composite request, Composite reply) {

  }

  @Override
  public void completed(Composite request, Composite reply) {

  }

  @Override
  public void failed(Composite request, Composite reply) {

  }

  private Composite createChain(byte[] encoded, Certificate[] issuerChain, int validityDays) {

    try {

      final PKCS10CertificationRequest csr = new PKCS10CertificationRequest(encoded);

      final KeyFactory keyFactory = KeyFactory.getInstance(
          csr.getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm().toString());

      final X509EncodedKeySpec subjectKeySpec =
          new X509EncodedKeySpec(csr.getSubjectPublicKeyInfo().getEncoded());

      final Certificate issuerCert = issuerChain[0];

      final int coseAlg = getCryptoService().getCoseAlgorithm(issuerCert.getPublicKey());
      final String sigAlg = getCryptoService().getSignatureAlgorithm(coseAlg);

      JcaContentSignerBuilder jcaBuilder = new JcaContentSignerBuilder(sigAlg)
          .setSecureRandom(getCryptoService().getSecureRandom());

      X509CertificateHolder certHolder = null;

      try (CloseableKey signingKey = resolver.getPrivateKey(issuerCert)) {
        ContentSigner signer = signer = jcaBuilder.build(signingKey.get());

        final X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            new X509CertificateHolder(issuerCert.getEncoded()).getSubject(),
            BigInteger.valueOf(System.currentTimeMillis()),
            Date.from(Instant.now()),
            Date.from(ZonedDateTime.now().plusDays(validityDays).toInstant()),
            csr.getSubject(),
            csr.getSubjectPublicKeyInfo());

        certHolder = certBuilder.build(signer);
      }

      byte[] leaf = new JcaX509CertificateConverter()
          .getCertificate(certHolder).getEncoded();

      Composite chain = Composite.newArray();
      chain.set(Const.FIRST_KEY, leaf);
      for (Certificate cert : issuerChain) {
        chain.set(chain.size(), cert.getEncoded());
      }

      getCryptoService().verify(chain);
      return chain;

    } catch (NoSuchAlgorithmException | IOException | OperatorCreationException
        | CertificateEncodingException e) {
      throw new DispatchException(e);
    } catch (CertificateException e) {
      throw new DispatchException(e);
    }
  }

  private Composite getSettings() {

    Composite result = Composite.newArray();

    String sql = "SELECT "
        + "CERTIFICATE_VALIDITY_DAYS, "
        + "RENDEZVOUS_INFO, "
        + "AUTO_ASSIGN_CUSTOMER_ID "
        + "FROM MT_SETTINGS WHERE ID=1";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          result.set(Const.FIRST_KEY, rs.getInt(1));
          result.set(Const.SECOND_KEY, rs.getString(2));
          result.set(Const.THIRD_KEY, rs.getString(3));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private CryptoService getCryptoService() {
    return cryptoService;
  }

  protected static String getToken(Composite request) {
    Composite protocolInfo = request.getAsComposite(Const.SM_PROTOCOL_INFO);
    if (!protocolInfo.containsKey(Const.PI_TOKEN)) {
      throw new InvalidJwtException();
    }
    return protocolInfo.getAsString(Const.PI_TOKEN);
  }

}
