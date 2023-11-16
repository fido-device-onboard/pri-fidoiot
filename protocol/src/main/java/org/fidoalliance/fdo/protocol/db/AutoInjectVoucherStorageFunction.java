// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpInstruction;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.entity.RvRedirect;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.Nonce;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To1dPayload;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Auto-injects voucher during DI to owner and RV database tables.
 */
public class AutoInjectVoucherStorageFunction extends StandardVoucherStorageFunction {

  public static LoggerService logger = new LoggerService(AutoInjectVoucherStorageFunction.class);

  /**
   * Constructor.
   */
  public AutoInjectVoucherStorageFunction() {
    logger.info("Voucher auto-injection enabled.");
  }

  @Override
  public UUID apply(String serialNo, OwnershipVoucher ownershipVoucher) throws IOException {
    super.apply(serialNo, ownershipVoucher);
    final To2AddressEntries to2Entries = new To2BlobSupplier().get();
    final Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {
      trans = session.beginTransaction();

      OwnerPublicKey lastOwner = VoucherUtils.getLastOwner(ownershipVoucher);
      final KeyResolver ownerResolver = Config.getWorker(OwnerKeySupplier.class).get();

      PublicKey lastKey = Config.getWorker(CryptoService.class).decodeKey(lastOwner);
      final String alias = KeyResolver.getAlias(lastOwner.getType(),
          new AlgorithmFinder().getKeySizeType(lastKey));

      final Certificate[] newOwnerChain = ownerResolver.getCertificateChain(alias);
      VoucherUtils.extend(ownershipVoucher,
          Config.getWorker(ManufacturerKeySupplier.class).get(),
          newOwnerChain
      );

      OnboardingVoucher onboardingVoucher = new OnboardingVoucher();
      Guid guid = VoucherUtils.getGuid(ownershipVoucher);
      onboardingVoucher.setGuid(guid.toString());
      logger.info("Onboarding Voucher GUID is : " + guid.toString());
      onboardingVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
      onboardingVoucher.setData(Mapper.INSTANCE.writeValue(ownershipVoucher));

      OwnershipVoucherHeader header = VoucherUtils.getHeader(ownershipVoucher);
      List<HttpInstruction> h1 = HttpUtils.getInstructions(header.getRendezvousInfo(),
          false);

      List<HttpInstruction> h2 = HttpUtils.getInstructions(to2Entries);

      if (!HttpUtils.containsAddress(h1, h2)) {
        logger.warn("RVInfo and T02Blob addresses is not matching."
                + " Make sure both entries are correct.");
      }

      To0d to0d = new To0d();
      to0d.setVoucher(ownershipVoucher);
      to0d.setWaitSeconds(Duration.ofDays(360).toSeconds());
      to0d.setNonce(Nonce.fromRandomUuid());

      HashType hashType = new AlgorithmFinder().getCompatibleHashType(
          ownershipVoucher.getHmac().getHashType());

      CryptoService cs = Config.getWorker(CryptoService.class);
      byte[] to0dBytes = Mapper.INSTANCE.writeValue(to0d);
      Hash to0dHash = cs.hash(hashType, to0dBytes);

      To1dPayload to1dPayload = new To1dPayload();
      to1dPayload.setAddressEntries(to2Entries);
      to1dPayload.setTo1ToTo0Hash(to0dHash);

      PrivateKey privateKey = ownerResolver.getPrivateKey(newOwnerChain[0].getPublicKey());
      try {
        CoseSign1 sign1 = cs.sign(
            Mapper.INSTANCE.writeValue(to1dPayload),
            privateKey,
            lastOwner
        );
        RvRedirect blob = new RvRedirect();
        blob.setGuid(header.getGuid().toString());

        To2RedirectEntry redirectEntry = new To2RedirectEntry();
        redirectEntry.setTo1d(sign1);
        redirectEntry.setCertChain(ownershipVoucher.getCertChain());
        blob.setData(Mapper.INSTANCE.writeValue(redirectEntry));

        Date expiry = new Date(System.currentTimeMillis()
            + Duration.ofSeconds(to0d.getWaitSeconds()).toMillis());
        blob.setCreatedOn(new Date(System.currentTimeMillis()));
        blob.setExpiry(expiry);
        onboardingVoucher.setTo0Expiry(expiry);

        session.persist(blob);
        logger.info("Voucher auto injected for guid:" + header.getGuid().toString());

      } finally {
        logger.debug("Destroying private key");
        cs.destroyKey(privateKey);
      }

      session.persist(onboardingVoucher);

      trans.commit();
      return VoucherUtils.getGuid(ownershipVoucher).toUuid();
    } finally {

      session.close();
    }
  }
}
