// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidJwtException;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.ResourceNotFoundException;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.protocol.To2ServerStorage;
import org.fido.iot.protocol.cbor.Encoder;
import org.fido.iot.serviceinfo.ServiceInfo;
import org.fido.iot.serviceinfo.ServiceInfoEntry;
import org.fido.iot.serviceinfo.ServiceInfoMarshaller;
import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class OwnerDbStorage implements To2ServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final KeyResolver keyResolver;
  private Composite ownerState;
  private UUID guid;
  private String cipherName;
  private byte[] nonce6;
  private byte[] nonce7;
  private Composite voucher;
  private String sessionId;
  private byte[] replacementHmac;
  private Composite sigInfoA;
  private long serviceInfoCount = 0;
  private long serviceInfoPosition = 0;
  private ServiceInfoMarshaller serviceInfoMarshaller;

  private static final int SERVICEINFO_MTU = 1300;

  /**
   * Constructs a OwnerDbStorage instance.
   *
   * @param cs          A cryptoService.
   * @param ds          A SQL datasource.
   * @param keyResolver A key resolver.
   */
  public OwnerDbStorage(CryptoService cs, DataSource ds, KeyResolver keyResolver) {
    cryptoService = cs;
    dataSource = ds;
    this.keyResolver = keyResolver;
  }

  @Override
  public PrivateKey getOwnerSigningKey(PublicKey key) {
    return keyResolver.getKey(key);
  }

  @Override
  public byte[] getNonce6() {
    return nonce6;
  }

  @Override
  public void setNonce6(byte[] nonce) {
    nonce6 = nonce;
  }

  @Override
  public void setOwnerState(Composite ownerState) {
    this.ownerState = ownerState;
  }

  @Override
  public Composite getOwnerState() {
    return ownerState;
  }

  @Override
  public void setCipherName(String cipherName) {
    this.cipherName = cipherName;
  }

  @Override
  public String getCipherName() {
    return cipherName;
  }

  @Override
  public void setGuid(UUID guid) {
    this.guid = guid;
    voucher = getVoucher();
  }

  @Override
  public Composite getSigInfoA() {
    return sigInfoA;
  }

  @Override
  public void setSigInfoA(Composite sigInfoA) {
    this.sigInfoA = sigInfoA;
  }

  @Override
  public Composite getVoucher() {
    if (voucher == null) {
      String sql = "SELECT VOUCHER FROM TO2_DEVICES WHERE GUID = ?;";

      try (Connection conn = dataSource.getConnection();
          PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, guid.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            voucher = Composite.fromObject(rs.getBinaryStream(1));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    if (voucher == null) {
      throw new ResourceNotFoundException(guid.toString());
    }
    return voucher;
  }

  @Override
  public void storeVoucher(Composite replacementVoucher) {
    if (guid == null) {
      guid = voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
    }
    String sql = "UPDATE TO2_DEVICES "
        + "SET REPLACEMENT_VOUCHER = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, replacementVoucher.toBytes());
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setNonce7(byte[] nonce7) {
    this.nonce7 = nonce7;
    String sql = "UPDATE TO2_SESSIONS "
        + "SET NONCE7 = ?,"
        + "UPDATED = ? "
        + "WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, nonce7);
      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(2, updatedAt);
      pstmt.setString(3, sessionId);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public byte[] getNonce7() {
    return nonce7;
  }

  @Override
  public Composite getReplacementRvInfo() {
    if (guid == null) {
      guid = voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
    }
    String sql = "SELECT REPLACEMENT_RVINFO FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          InputStream is = rs.getBinaryStream(1);
          if (is != null) {
            return Composite.fromObject(is);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    // by default, return the same rv info
    return voucher
        .getAsComposite(Const.OV_HEADER)
        .getAsComposite(Const.OVH_RENDEZVOUS_INFO);
  }

  @Override
  public UUID getReplacementGuid() {
    if (guid == null) {
      guid = voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
    }
    String sql = "SELECT REPLACEMENT_GUID FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          String uuid = rs.getString(1);
          if (uuid != null) {
            return UUID.fromString(uuid);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    // by default, return the same guid for reuse scenario
    return guid;
  }

  @Override
  public Composite getReplacementOwnerKey() {
    return getCryptoService().getOwnerPublicKey(voucher);
  }

  @Override
  public void discardReplacementOwnerKey() {
  }

  @Override
  public boolean getOwnerResaleSupport() {
    return true;
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    sessionId = getToken(request);

    if (sessionId.isEmpty()) {
      throw new InvalidJwtException();
    }

    String sql = "SELECT VOUCHER, OWNER_STATE, CIPHER_NAME, NONCE6, NONCE7, SIGINFOA, "
        + "SERVICEINFO_COUNT, SERVICEINFO_POSITION, SERVICEINFO_BLOB "
        + "FROM TO2_SESSIONS WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          voucher = Composite.fromObject(rs.getBinaryStream(1));
          ownerState = Composite.fromObject(rs.getBinaryStream(2));
          cipherName = rs.getString(3);
          nonce6 = rs.getBytes(4);
          nonce7 = rs.getBytes(5);
          sigInfoA = Composite.fromObject(rs.getBinaryStream(6));
          serviceInfoCount = rs.getLong(7);
          serviceInfoPosition = rs.getLong(8);
          if (rs.getBlob(9) != null) {
            ObjectInputStream inputStream = new ObjectInputStream(
                rs.getBlob(9).getBinaryStream());
            serviceInfoMarshaller = (ServiceInfoMarshaller) inputStream.readObject();
            inputStream.close();
          }
        }
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException();
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (voucher == null) {
      throw new InvalidJwtException(sessionId);
    }

  }

  @Override
  public Composite getNextServiceInfo() {
    if (serviceInfoCount == 0 || serviceInfoPosition == serviceInfoCount) {
      return ServiceInfoEncoder.encodeOwnerServiceInfo(Collections.EMPTY_LIST, false, true);
    } else {
      serviceInfoMarshaller.register(new OwnerServiceInfoModule(dataSource));
      Iterable<Supplier<ServiceInfo>> serviceInfos = serviceInfoMarshaller.marshal();
      List<Composite> svi = new LinkedList<Composite>();
      final Iterator<Supplier<ServiceInfo>> it = serviceInfos.iterator();
      if (it.hasNext()) {
        ServiceInfo serviceInfo = it.next().get();
        Iterator<ServiceInfoEntry> marshalledEntries = serviceInfo.iterator();
        while (marshalledEntries.hasNext()) {
          ServiceInfoEntry marshalledEntry = marshalledEntries.next();
          Composite innerArray = ServiceInfoEncoder.encodeValue(marshalledEntry.getKey(),
                marshalledEntry.getValue().getContent());
          svi.add(innerArray);
        }
        ++serviceInfoPosition;
      }
      return ServiceInfoEncoder.encodeOwnerServiceInfo(svi, true, false);
    }
  }

  @Override
  public void setServiceInfo(Composite info, boolean isMore) {
    new OwnerServiceInfoModule(dataSource).putServiceInfo(
        voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID),
        new ServiceInfoEntry(info.getAsString(Const.FIRST_KEY),
            new ServiceInfoSequence(info.getAsString(Const.FIRST_KEY)) {

              @Override
              public long length() {
                // Return 0 as length is not supposed to be used anywhere
                return 0;
              }

              @Override
              public Object getContent() {
                // Returns CBOR encoded data so that it can be written into the database as bytes
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                WritableByteChannel wbc = Channels.newChannel(out);
                Encoder encoder = new Encoder.Builder(wbc).build();
                try {
                  encoder.writeObject(info.get(Const.SECOND_KEY));
                } catch (IOException e) {
                  System.out.println("Invalid serviceinfo value" + e.getMessage());
                }
                return out.toByteArray();
              }

              @Override
              public boolean canSplit() {
                return false;
              }
            }));
  }

  @Override
  public byte[] getReplacementHmac() {
    if (guid == null) {
      guid = voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
    }
    String sql = "SELECT REPLACEMENT_HMAC FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          replacementHmac = rs.getBytes(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return this.replacementHmac;
  }

  @Override
  public void setReplacementHmac(byte[] hmac) {
    if (guid == null) {
      guid = voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
    }
    String sql = "UPDATE TO2_DEVICES "
        + "SET REPLACEMENT_HMAC = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, hmac);
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void prepareServiceInfo() {
    serviceInfoMarshaller = new ServiceInfoMarshaller(SERVICEINFO_MTU,
        voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID));
    serviceInfoMarshaller.register(new OwnerServiceInfoModule(dataSource));
    Iterable<Supplier<ServiceInfo>> serviceInfo = serviceInfoMarshaller.marshal();
    int mtuPacketCount = 0;
    for (final Iterator<Supplier<ServiceInfo>> it = serviceInfo.iterator(); it.hasNext();) {
      it.next().get();
      ++mtuPacketCount;
    }
    serviceInfoCount = mtuPacketCount;
    serviceInfoPosition = 0;
    // Reset the positions because we need to start from the beginning to send service info.
    serviceInfoMarshaller.reset();
  }

  @Override
  public void starting(Composite request, Composite reply) {
    String uuid = request.getAsComposite(Const.SM_BODY).getAsUuid(Const.FIRST_KEY).toString();
    String sql = "UPDATE TO2_DEVICES "
        + "SET TO2_STARTED = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(1, created);
      pstmt.setString(2, uuid);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void started(Composite request, Composite reply) {
    String sessionId = UUID.randomUUID().toString();
    reply.set(Const.SM_PROTOCOL_INFO,
        Composite.newMap().set(Const.PI_TOKEN, sessionId));

    String sql = "INSERT INTO TO2_SESSIONS "
        + "(SESSION_ID,"
        + "VOUCHER,"
        + "OWNER_STATE,"
        + "CIPHER_NAME, "
        + "NONCE6,"
        + "NONCE7,"
        + "SIGINFOA,"
        + "CREATED,"
        + "UPDATED) "
        + "VALUES (?,?,?,?,?,?,?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);
      pstmt.setBytes(2, voucher.toBytes());
      pstmt.setBytes(3, ownerState.toBytes());
      pstmt.setString(4, cipherName);
      pstmt.setBytes(5, nonce6);
      pstmt.setBytes(6, nonce7);
      pstmt.setBytes(7, sigInfoA.toBytes());
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(8, created);
      pstmt.setTimestamp(9, created);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void completed(Composite request, Composite reply) {

    String sql = "DELETE FROM TO2_SESSIONS WHERE SESSION_ID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sessionId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    sql = "UPDATE TO2_DEVICES "
        + "SET TO2_COMPLETED = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(1, created);
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void failed(Composite request, Composite reply) {

  }

  @Override
  public void continued(Composite request, Composite reply) {
    String sql = "UPDATE TO2_SESSIONS "
        + "SET OWNER_STATE = ? ,"
        + "SERVICEINFO_COUNT = ? ,"
        + "SERVICEINFO_POSITION = ? ,"
        + "SERVICEINFO_BLOB = ? ,"
        + "UPDATED = ? "
        + "WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, ownerState.toBytes());
      pstmt.setLong(2, serviceInfoCount);
      pstmt.setLong(3, serviceInfoPosition);
      if (serviceInfoMarshaller != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(
            baos);
        outputStream.writeObject(serviceInfoMarshaller);
        pstmt.setBytes(4,  baos.toByteArray());
        outputStream.close();
      } else {
        pstmt.setObject(4,  null);
      }
      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(5, updatedAt);
      pstmt.setString(6, sessionId);

      pstmt.executeUpdate();

    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
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
