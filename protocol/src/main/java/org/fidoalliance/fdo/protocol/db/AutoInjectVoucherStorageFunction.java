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
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To1dPayload;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AutoInjectVoucherStorageFunction extends StandardVoucherStorageFunction {

  @Override
  public UUID apply(String serialNo, OwnershipVoucher ownershipVoucher) throws IOException {
    super.apply(serialNo, ownershipVoucher);
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {

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

      Transaction trans = session.beginTransaction();

      OnboardingVoucher onboardingVoucher = new OnboardingVoucher();
      Guid guid = VoucherUtils.getGuid(ownershipVoucher);
      onboardingVoucher.setGuid(guid.toString());
      onboardingVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
      onboardingVoucher.setData(Mapper.INSTANCE.writeValue(ownershipVoucher));
      session.persist(onboardingVoucher);

      OwnershipVoucherHeader header = VoucherUtils.getHeader(ownershipVoucher);
      List<HttpInstruction> h1 = HttpUtils.getInstructions(header.getRendezvousInfo(),
          false);

      OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();
      To2AddressEntries to2Entries = Mapper.INSTANCE.readValue(
          onboardConfig.getRvBlob(), To2AddressEntries.class);
      List<HttpInstruction> h2 = HttpUtils.getInstructions(to2Entries);
      if (HttpUtils.containsAddress(h1, h2)) {
        To0d to0d = new To0d();
        to0d.setVoucher(ownershipVoucher);
        to0d.setWaitSeconds(Duration.ofDays(360 * 10).toSeconds());
        to0d.setNonce(Nonce.fromRandomUUID());

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

          blob.setCreatedOn(new Date(System.currentTimeMillis()));
          blob.setExpiry(
              new Date(System.currentTimeMillis()
                  + Duration.ofSeconds(to0d.getWaitSeconds()).toMillis()));

          session.persist(blob);
        } finally {
          cs.destroyKey(privateKey);
        }
      }

      trans.commit();

      return VoucherUtils.getGuid(ownershipVoucher).toUuid();
    } finally {
      session.close();
    }
  }
}
