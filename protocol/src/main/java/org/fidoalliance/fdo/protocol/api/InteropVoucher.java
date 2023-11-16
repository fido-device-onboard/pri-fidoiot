// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.UUID;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemFormatter;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ExtraInfoSupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntries;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntryPayload;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class InteropVoucher extends RestApi {

  private static final LoggerService logger = new LoggerService(InteropVoucher.class);

  private static final String OV_PEM_HEADER = "-----BEGIN OWNERSHIP VOUCHER-----";
  private static final String OV_PEM_FOOTER = "-----END OWNERSHIP VOUCHER-----";


  @Override
  protected void doPost() throws Exception {

    try {

      String pemString = getStringBody();

      OwnershipVoucher voucher = null;
      UUID guid = null;
      PrivateKey signKey = null;

      try (StringReader reader = new StringReader(pemString);
          PEMParser parser = new PEMParser(reader)) {
        for (; ; ) {
          Object obj = parser.readPemObject();
          if (obj == null) {
            break;
          }
          PemObject pemObj = (PemObject) obj;
          if (pemObj.getType().equals("OWNERSHIP VOUCHER")) {

            voucher = Mapper.INSTANCE.readValue(pemObj.getContent(), OwnershipVoucher.class);
            OwnershipVoucherHeader header =
                Mapper.INSTANCE.readValue(voucher.getHeader(), OwnershipVoucherHeader.class);

            guid = header.getGuid().toUuid();
            logger.info("voucher guid: " + guid.toString());

            String devicePem = PemFormatter.format(voucher.getCertChain().getChain());
            logger.info(devicePem);

          } else if (pemObj.getType().equals("EC PRIVATE KEY")
              || pemObj.getType().equals("RSA PRIVATE KEY")) {
            ASN1Sequence seq = ASN1Sequence.getInstance(pemObj.getContent());

            PrivateKeyInfo info = PrivateKeyInfo.getInstance(seq);
            signKey = new JcaPEMKeyConverter().getPrivateKey(info);

          }
        }
      }

      //we should have voucher and private key
      if (voucher != null) {
        logger.info("decoded voucher from pem");
      } else {
        logger.warn("unable to decode voucher from pem");
        getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      if (signKey != null) {
        logger.info("decoded private key from pem");
      } else {
        logger.warn("unable to decode private key from pem");
      }

      CryptoService cs = Config.getWorker(CryptoService.class);
      KeyResolver resolver = Config.getWorker(OwnerKeySupplier.class).get();
      OwnerPublicKey prevKey = VoucherUtils.getLastOwner(voucher);
      String alias = KeyResolver.getAlias(prevKey.getType(),
          new AlgorithmFinder().getKeySizeType(cs.decodeKey(prevKey)));
      Certificate[] certs = resolver.getCertificateChain(alias);

      extend(voucher, signKey, certs);

      getTransaction();
      OnboardingVoucher dbVoucher = getSession().get(OnboardingVoucher.class, guid.toString());
      if (dbVoucher == null) {
        dbVoucher = new OnboardingVoucher();
        dbVoucher.setGuid(guid.toString());
        dbVoucher.setData(Mapper.INSTANCE.writeValue(voucher));
        dbVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
        getSession().save(dbVoucher);
      } else {
        dbVoucher.setData(Mapper.INSTANCE.writeValue(voucher));
        getSession().update(dbVoucher);
      }
      //save the voucher
      //todo: need to do TO0 manually

      //write the guid response
      byte[] guidResponse = guid.toString().getBytes(StandardCharsets.UTF_8);
      getResponse().setContentLength(guidResponse.length);
      getResponse().getOutputStream().write(guidResponse);
    } catch (Exception e) {
      logger.warn("Request failed because of internal server error.");
      getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doGet() throws Exception {

    try {

      String id = getLastSegment();

      if (id != null) {

        OwnershipVoucher voucher = Config.getWorker(VoucherQueryFunction.class).apply(id);

        if (voucher != null) {
          String pemString = VoucherUtils.toString(Mapper.INSTANCE.writeValue(voucher))
              + getFormattedKey(voucher);

          getResponse().getWriter().print(pemString);
        } else {
          logger.warn("Request failed unknown uuid.");
          getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      } else {
        getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
    } catch (RuntimeException e) {
      logger.warn("Failed due to Runtime Exception" + e.getMessage());
      getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }


  private String getKeyFormatString(OwnerPublicKey ownerKey) {

    String result = "PRIVATE KEY";
    switch (ownerKey.getType()) {
      case SECP256R1:
      case SECP384R1:
        result = "EC PRIVATE KEY";
        break;
      case RSA2048RESTR:
      case RSAPKCS:
        result = "RSA PRIVATE KEY";
        break;
      default:
        break;
    }

    return result;
  }

  private String getFormattedKey(OwnershipVoucher voucher) {
    try {

      OwnerPublicKey pubKey = VoucherUtils.getLastOwner(voucher);

      KeyResolver resolver = Config.getWorker(OwnerKeySupplier.class).get();
      CryptoService cs = Config.getWorker(CryptoService.class);

      PrivateKey pvt = resolver.getPrivateKey(cs.decodeKey(pubKey));

      StringWriter writer = new StringWriter();
      PemWriter pemWriter = new PemWriter(writer);
      pemWriter.writeObject(new PemObject(getKeyFormatString(pubKey), pvt.getEncoded()));
      pemWriter.flush();
      pemWriter.close();
      return "\r\n" + writer;


    } catch (Exception e) {
      logger.warn(e.getMessage());
      return "";
    }
  }

  private OwnershipVoucher extend(OwnershipVoucher voucher,
      PrivateKey signingKey, Certificate[] nextChain)
      throws Exception {

    Hash mac = voucher.getHmac();
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(mac.getHashType());
    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(voucher.getHeader(), OwnershipVoucherHeader.class);

    CryptoService cs = Config.getWorker(CryptoService.class);
    Hash hdrHash = VoucherUtils.getHeaderHash(hashType, header);
    Hash prevHash;
    OwnerPublicKey prevOwnerPubKey;

    OwnershipVoucherEntries entries = voucher.getEntries();
    if (entries.size() == 0) {
      throw new InternalServerErrorException(new IllegalArgumentException());
    } else {
      CoseSign1 entry = entries.getLast();

      OwnershipVoucherEntryPayload entryPayload =
          Mapper.INSTANCE.readValue(entry.getPayload(), OwnershipVoucherEntryPayload.class);

      byte[] prevBytes = Mapper.INSTANCE.writeValue(entry);
      prevHash = cs.hash(hashType, prevBytes);
      prevOwnerPubKey = entryPayload.getOwnerPublicKey();
    }

    OwnershipVoucherEntryPayload entryPayload = new OwnershipVoucherEntryPayload();
    entryPayload.setPreviousHash(prevHash);
    entryPayload.setHeaderHash(hdrHash);
    entryPayload.setExtra(Config.getWorker(ExtraInfoSupplier.class).get());

    OwnerPublicKey nextOwnerKey = cs.encodeKey(prevOwnerPubKey.getType(),
        prevOwnerPubKey.getEnc(),
        nextChain);

    //assume owner is encoded same a
    entryPayload.setOwnerPublicKey(nextOwnerKey);
    byte[] payload = Mapper.INSTANCE.writeValue(entryPayload);

    try {
      CoseSign1 nextEntry = cs.sign(payload, signingKey, prevOwnerPubKey);
      entries.add(nextEntry);
      boolean bok = cs.verify(nextEntry, prevOwnerPubKey);
      if (!bok) {
        throw new InternalServerErrorException(new SignatureException());
      }
    } finally {
      cs.destroyKey(signingKey);
    }
    return voucher;
  }


}
