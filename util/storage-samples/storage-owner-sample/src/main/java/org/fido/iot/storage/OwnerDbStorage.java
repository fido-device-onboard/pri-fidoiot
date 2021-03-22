// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidJwtException;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.ResourceNotFoundException;
import org.fido.iot.protocol.To2ServerStorage;
import org.fido.iot.protocol.ondie.OnDieService;
import org.fido.iot.serviceinfo.ModuleManager;

public class OwnerDbStorage implements To2ServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final KeyResolver keyResolver;
  private OnDieService onDieService;
  private Composite ownerState;
  private String cipherName;
  private byte[] nonce6;
  private byte[] nonce7;
  private Composite voucher;
  private String sessionId;
  private byte[] replacementHmac;
  private Composite sigInfoA;
  int ownerServiceInfoMtuSize = 0;
  String deviceServiceInfoMtuSize = String.valueOf(0);
  ModuleManager modules;


  /**
   * Constructs a OwnerDbStorage instance.
   *
   * @param cs          A cryptoService.
   * @param ds          A SQL datasource.
   * @param keyResolver A key resolver.
   */
  public OwnerDbStorage(CryptoService cs,
      DataSource ds,
      KeyResolver keyResolver,
      OnDieService ods) {
    cryptoService = cs;
    dataSource = ds;
    this.keyResolver = keyResolver;
    onDieService = ods;
    modules = new ModuleManager();
    modules.addModule(new OwnerDevMod(ds));
    modules.addModule(new OwnerSysModule(ds));
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
    loadVoucher(guid);
  }

  private UUID getGuid() {
    return voucher.getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID);
  }


  private void loadVoucher(UUID guid) {
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

    if (voucher == null) {
      throw new ResourceNotFoundException(guid.toString());
    }
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
    return voucher;
  }

  @Override
  public void storeVoucher(Composite replacementVoucher) {

    String sql = "UPDATE TO2_DEVICES "
        + "SET REPLACEMENT_VOUCHER = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, replacementVoucher.toBytes());
      pstmt.setString(2, getGuid().toString());

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
  public OnDieService getOnDieService() {
    return onDieService;
  }

  @Override
  public Composite getReplacementRvInfo() {
    String sql = "SELECT REPLACEMENT_RVINFO FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());
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

    String sql = "SELECT REPLACEMENT_GUID FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());
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
    return getGuid();
  }

  @Override
  public Composite getReplacementOwnerKey() {
    List<PublicKey> certs = null;

    String sql =
        "SELECT KEYS FROM OWNER_CUSTOMERS WHERE CUSTOMER_ID =  "
            + "(SELECT CUSTOMER_ID FROM TO2_DEVICES WHERE GUID = ?)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          certs = PemLoader.loadPublicKeys(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      /* If no customerId is found in the DB,
      current ownerkey is returned as replacement owner key.
      The scenario will default to reuse case.
      */
      return getCryptoService().getOwnerPublicKey(voucher);
    }

    int keyType =
        getCryptoService()
            .getPublicKeyType(
                getCryptoService().decode(getCryptoService().getOwnerPublicKey(voucher)));

    PublicKey ownerPub = null;

    for (PublicKey pub : certs) {
      int ownerType = getCryptoService().getPublicKeyType(pub);
      if (ownerType == keyType) {
        ownerPub = pub;
        break;
      }
    }

    return getCryptoService().encode(ownerPub,
        getCryptoService().getCompatibleEncoding(ownerPub));
  }

  @Override
  public void discardReplacementOwnerKey() {
  }

  @Override
  public boolean getOwnerResaleSupport() {
    return true;
  }

  // maximum size service info that owner can receive from device (i.e) DeviceServiceInfoMTU
  @Override
  public String getMaxDeviceServiceInfoMtuSz() {

    String sql = "SELECT DEVICE_SERVICE_INFO_MTU_SIZE FROM TO2_SETTINGS WHERE ID = 1;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          if (rs.getInt(1) < 0) {
            System.out.println("Negative value received. MTU size will default to 1300 bytes");
          }
          deviceServiceInfoMtuSize = (rs.getInt(1) > 0 ? String.valueOf(rs.getInt(1)) : null);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return deviceServiceInfoMtuSize;
  }

  @Override
  public void setMaxOwnerServiceInfoMtuSz(int mtu) {

    String sql = "UPDATE TO2_DEVICES "
        + "SET OWNER_SERVICE_INFO_MTU_SIZE = ? "
        + "WHERE GUID = ?";

    modules.setMtu(mtu);

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, mtu);
      pstmt.setString(2, getGuid().toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // maximum size service info that owner can send to device (i.e) OwnerServiceInfoMTU
  @Override
  public int getMaxOwnerServiceInfoMtuSz() {

    int ownerMtuThreshold = 0;
    String sql = "SELECT OWNER_MTU_THRESHOLD FROM TO2_SETTINGS WHERE ID = 1;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);) {
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          ownerMtuThreshold = rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    sql = "SELECT OWNER_SERVICE_INFO_MTU_SIZE FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          ownerServiceInfoMtuSize = rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    if (ownerMtuThreshold < ownerServiceInfoMtuSize) {
      System.out.println(
          "Device Maximum MTU request exceeds, owner threshold MTU size. "
              + "MTU size set to owner threshold MTU size");
    }

    return Math.min(ownerServiceInfoMtuSize, ownerMtuThreshold);
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    sessionId = getToken(request);

    if (sessionId.isEmpty()) {
      throw new InvalidJwtException();
    }

    String sql = "SELECT VOUCHER, OWNER_STATE, CIPHER_NAME, NONCE6, NONCE7, SIGINFOA, "
        + " SERVICEINFO_BLOB "
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
          if (rs.getBlob(7) != null) {
            try (InputStream inputStream = rs.getBlob(7).getBinaryStream()) {
              modules.setState(Composite.fromObject(inputStream));
            }
          }
        }
      } catch (IOException e) {
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
    return modules.getNextServiceInfo();
  }

  @Override
  public void setServiceInfo(Composite info, boolean isMore) {
    modules.setServiceInfo(info, isMore);

  }

  @Override
  public byte[] getReplacementHmac() {

    String sql = "SELECT REPLACEMENT_HMAC FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());
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

    String sql = "UPDATE TO2_DEVICES "
        + "SET REPLACEMENT_HMAC = ? "
        + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, hmac);
      pstmt.setString(2, getGuid().toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void prepareServiceInfo() {
    modules.prepare(getGuid());
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

    deleteTo2Session();

    String sql =
        "UPDATE TO2_DEVICES "
            + "SET TO2_COMPLETED = ? "
            + "WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(1, created);
      pstmt.setString(2, getGuid().toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void failed(Composite request, Composite reply) {
    deleteTo2Session();
  }

  @Override
  public void continued(Composite request, Composite reply) {
    String sql = "UPDATE TO2_SESSIONS "
        + "SET OWNER_STATE = ? ,"
        + "SERVICEINFO_BLOB = ? ,"
        + "UPDATED = ? "
        + "WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, ownerState.toBytes());
      pstmt.setBytes(2, modules.getState().toBytes());

      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(3, updatedAt);
      pstmt.setString(4, sessionId);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteTo2Session() {
    String sql = "DELETE FROM TO2_SESSIONS WHERE SESSION_ID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sessionId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
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
