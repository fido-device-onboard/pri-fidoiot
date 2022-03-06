// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.fidoalliance.fdo.protocol.api.RvInfo;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ExtraInfoSupplier;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntries;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntryPayload;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.RendezvousDirective;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionDeserializer;

/**
 * Ownership Voucher utilities.
 */
public class VoucherUtils {


  private static final String OV_PEM_HEADER = "-----BEGIN OWNERSHIP VOUCHER-----";
  private static final String OV_PEM_FOOTER = "-----END OWNERSHIP VOUCHER-----";


  /**
   * Extends and ownership voucher
   *
   * @param voucher     An instance of a Voucher.
   * @param keyResolver The key resolver for the last owner.
   * @param nextChain   The certificate chain for the next owner.
   * @return An extended voucher
   * @throws IOException An error occurred.
   */

  public static OwnershipVoucher extend(OwnershipVoucher voucher,
      KeyResolver keyResolver, Certificate[] nextChain)
      throws IOException {

    Hash mac = voucher.getHmac();
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(mac.getHashType());
    byte[] headerTag = voucher.getHeader();
    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(headerTag,OwnershipVoucherHeader.class);

    CryptoService cs = Config.getWorker(CryptoService.class);
    Hash hdrHash = getHeaderHash(hashType, header);
    Hash prevHash;
    OwnerPublicKey prevOwnerPubKey;

    OwnershipVoucherEntries entries = voucher.getEntries();
    if (entries.size() == 0) {
      prevHash = getEntryHash(mac,headerTag);
      prevOwnerPubKey = header.getPublicKey();
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

    PublicKey publicKey = cs.decodeKey(prevOwnerPubKey);
    PrivateKey signingKey = keyResolver.getPrivateKey(publicKey);
    try {
      CoseSign1 nextEntry = cs.sign(payload, signingKey, prevOwnerPubKey);
      entries.add(nextEntry);
      boolean bok = cs.verify(nextEntry, prevOwnerPubKey);
    } finally {
      cs.destroyKey(signingKey);
    }
    return voucher;
  }

  /**
   * Decodes the voucher from PEM String.
   *
   * @param pemString A Pem encoded Voucher
   * @return The Ownership Voucher Instance.
   * @throws IOException An Error occurred decoding the voucher.
   */
  public static OwnershipVoucher fromString(String pemString) throws IOException {
    try (StringReader reader = new StringReader(pemString);
        PEMParser parser = new PEMParser(reader)) {
      for (; ; ) {
        Object obj = parser.readPemObject();
        if (obj == null) {
          break;
        }
        if (obj instanceof PemObject) {
          PemObject pemObj = (PemObject) obj;
          if (pemObj.getType().equals("OWNERSHIP VOUCHER")) {
            return Mapper.INSTANCE.readValue(pemObj.getContent(), OwnershipVoucher.class);

          }
        }
      }
    }
    throw new IOException("voucher not found in pem");
  }

  /**
   * Coverts the voucher to a PEM String
   *
   * @param voucherData The cbor bytes of the voucher.
   * @return The voucher as a PEM String.
   */
  public static String toString(byte[] voucherData) {
    StringBuilder builder = new StringBuilder();
    String enc = Base64.getEncoder().encodeToString(voucherData);

    builder.append(OV_PEM_HEADER);
    builder.append("\r\n");
    int start = 0;
    int end = start + 64;
    for (; ; ) {
      if (end > enc.length()) {
        end = enc.length();
      }
      String line = enc.substring(start, end);
      builder.append(line);
      builder.append("\r\n");
      start = end;
      end = start + 64;
      if (start >= enc.length()) {
        break;
      }
    }
    builder.append(OV_PEM_FOOTER);
    return builder.toString();
  }

  /**
   * Coverts the voucher to a PEM String
   *
   * @param voucher An instance of a Voucher.
   * @return The voucher as a PEM String.
   */
  public static String toString(OwnershipVoucher voucher) throws IOException {
    byte[] data = Mapper.INSTANCE.writeValue(OwnershipVoucher.class);
    return toString(data);
  }

  /**
   * Gets the GUID of the voucher.
   *
   * @param voucher An instance of a Voucher.
   * @return The Guid of the voucher.
   * @throws IOException An Error occurred decoding the voucher.
   */
  public static Guid getGuid(OwnershipVoucher voucher) throws IOException {
    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(voucher.getHeader(),OwnershipVoucherHeader.class);

    return header.getGuid();
  }

  /**
   * Get the last owner of the voucher.
   *
   * @param voucher An instance of a Voucher.
   * @return The Last owners public key.
   * @throws IOException An Error occurred decoding the voucher.
   */
  public static OwnerPublicKey getLastOwner(OwnershipVoucher voucher) throws IOException {

    OwnershipVoucherEntries entries = voucher.getEntries();
    if (entries.size() == 0) {
      OwnershipVoucherHeader header =
          Mapper.INSTANCE.readValue(voucher.getHeader(),OwnershipVoucherHeader.class);
      return header.getPublicKey();
    }
    CoseSign1 entry = entries.getLast();

    OwnershipVoucherEntryPayload entryPayload =
        Mapper.INSTANCE.readValue(entry.getPayload(), OwnershipVoucherEntryPayload.class);

    return entryPayload.getOwnerPublicKey();
  }

  /**
   * Gets the Ownership voucher header.
   *
   * @param voucher An OwnershipVoucher instance.
   * @return The OwnershipVoucher header.
   * @throws IOException An error occurred.
   */
  public static OwnershipVoucherHeader getHeader(OwnershipVoucher voucher) throws IOException {
    return Mapper.INSTANCE.readValue(voucher.getHeader(),OwnershipVoucherHeader.class);
  }


  /**
   * Computes the header hash.
   *
   * @param hashType The hash type to use.
   * @param header   The voucher header.
   * @return The hash of the header (GUID|DEVINFO).
   * @throws IOException An error occurred.
   */
  public static Hash getHeaderHash(HashType hashType, OwnershipVoucherHeader header)
      throws IOException {

    byte[] guid = header.getGuid().toBytes();
    byte[] devInfo = header.getDeviceInfo().getBytes(StandardCharsets.UTF_8);
    ByteBuffer headerInfo = ByteBuffer.allocate(guid.length + devInfo.length);
    headerInfo.put(guid);
    headerInfo.put(devInfo);
    headerInfo.flip();

    CryptoService cs = Config.getWorker(CryptoService.class);
    return cs.hash(hashType, BufferUtils.unwrap(headerInfo));

  }


  /**
   * Gets the first Entry Hash.
   * @param hmac The hmac of the voucher.
   * @param ovhBytes The ownership voucher header bytes.
   * @return The hash (HMAC|OVHEADER)
   * @throws IOException An Error occured.
   */
  public static Hash getEntryHash(Hash hmac, byte[] ovhBytes) throws IOException {

    byte[] macBytes = Mapper.INSTANCE.writeValue(hmac);
    ByteBuffer prevInfo = ByteBuffer.allocate(ovhBytes.length + macBytes.length);
    prevInfo.put(ovhBytes);
    prevInfo.put(macBytes);
    prevInfo.flip();
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(hmac.getHashType());
    CryptoService cs = Config.getWorker(CryptoService.class);
    return cs.hash(hashType, BufferUtils.unwrap(prevInfo));

  }


}
