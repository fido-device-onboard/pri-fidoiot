// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.MessageBodyException;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.serviceinfo.SdoSys;

/**
 * Owner Database Manager.
 */
public class OwnerDbManager {

  private static final String SVI_ARRAY_DELIMETER = ",";
  private static final String SVI_ENTRY_DELIMETER = "=";
  private static final String SVI_MODMSG_DELIMETER = ":";

  /**
   * Creates owner tables.
   *
   * @param dataSource A SQL datasource.
   */
  public void createTables(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql = "CREATE TABLE IF NOT EXISTS "
          + "TO2_DEVICES("
          + "GUID CHAR(36) PRIMARY KEY, "
          + "VOUCHER BLOB, "
          + "WAIT_SECONDS_REQUEST INT NOT NULL DEFAULT 3600, "
          + "WAIT_SECONDS_RESPONSE INT NULL DEFAULT NULL, "
          + "CREATED TIMESTAMP NOT NULL, "
          + "TO0_STARTED TIMESTAMP NULL DEFAULT NULL, "
          + "TO0_COMPLETED TIMESTAMP NULL DEFAULT NULL, "
          + "TO2_STARTED TIMESTAMP NULL DEFAULT NULL, "
          + "TO2_COMPLETED TIMESTAMP NULL DEFAULT NULL, "
          + "REPLACEMENT_GUID CHAR(36), "
          + "REPLACEMENT_RVINFO BLOB, "
          + "REPLACEMENT_HMAC BLOB, "
          + "REPLACEMENT_VOUCHER BLOB, "
          + "PRIMARY KEY (GUID), "
          + "UNIQUE (GUID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "TO2_SESSIONS("
          + "SESSION_ID CHAR(36) PRIMARY KEY, "
          + "VOUCHER BLOB, "
          + "OWNER_STATE BLOB,"
          + "CIPHER_NAME VARCHAR(36), "
          + "NONCE6 BINARY(16), "
          + "NONCE7 BINARY(16), "
          + "SIGINFOA BLOB, "
          + "SERVICEINFO_COUNT BIGINT, "
          + "SERVICEINFO_POSITION BIGINT, "
          + "SERVICEINFO_BLOB BLOB, "
          + "CREATED TIMESTAMP,"
          + "UPDATED TIMESTAMP,"
          + "PRIMARY KEY (SESSION_ID), "
          + "UNIQUE (SESSION_ID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "OWNER_SERVICEINFO("
          + "SVI_ID CHAR(36) PRIMARY KEY, "
          + "CONTENT BLOB, "
          + "CONTENT_LENGTH BIGINT, "
          + "CONTENT_TYPE CHAR(10), "
          + "PRIMARY KEY (SVI_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "GUID_OWNERSVI("
          + "GUID CHAR(36), "
          + "SVI_ID CHAR(36), "
          + "MODULE_NAME CHAR(36), "
          + "MESSAGE_NAME CHAR(36),"
          + "CREATED_AT TIMESTAMP, "
          + "PRIMARY KEY (GUID, SVI_ID),"
          + "FOREIGN KEY (GUID) references TO2_DEVICES(GUID) ON DELETE CASCADE, "
          + "FOREIGN KEY (SVI_ID) REFERENCES OWNER_SERVICEINFO(SVI_ID) ON DELETE CASCADE"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "GUID_DEVICEDSI("
          + "GUID CHAR(36) NOT NULL, "
          + "DSI_KEY CHAR(100) NOT NULL, "
          + "DSI_VALUE BLOB NOT NULL, "
          + "PRIMARY KEY (GUID, DSI_KEY), "
          + "FOREIGN KEY (GUID) REFERENCES TO2_DEVICES(GUID) ON DELETE CASCADE"
          + ");";

      stmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports ownership voucher.
   *
   * @param ds      A SQL datasource.
   * @param voucher The ownership voucher to import.
   */
  public void importVoucher(DataSource ds, Composite voucher) {

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);

    String sql = ""
        + "MERGE INTO TO2_DEVICES  "
        + "KEY (GUID) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      pstmt.setBytes(2, voucher.toBytes());
      pstmt.setInt(3, 3600);
      pstmt.setInt(4, 0);
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(5, created);
      pstmt.setTimestamp(6, null);
      pstmt.setTimestamp(7, null);
      pstmt.setTimestamp(8, null);
      pstmt.setTimestamp(9, null);
      pstmt.setString(10, guid.toString());
      pstmt.setBytes(11, ovh
          .getAsComposite(Const.OVH_RENDEZVOUS_INFO).toBytes());
      pstmt.setBytes(12, null);
      pstmt.setBytes(13, null);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Removes a Voucher.
   *
   * @param ds   A Datasource.
   * @param guid The device guid.
   * @return The number of database objects affected.
   */
  public int removeVoucher(DataSource ds, UUID guid) {

    int result = 0;

    String sql = ""
        + "DELETE FROM TO2_DEVICES  WHERE GUID = ?";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());

      result = pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  /**
   * Add service info to the database.
   * 
   * @param ds Datasource
   * @param serviceInfoId Serviceinfo Identifier
   * @param serviceInfo Serviceinfo as byte array
   * @param isCborData 'true', if data is cbor-encoded, 'false' otherwise
   */
  public void addServiceInfo(DataSource ds, String serviceInfoId, byte[] serviceInfo,
      boolean isCborData) {
    String sql = ""
        + "MERGE INTO OWNER_SERVICEINFO  "
        + "KEY (SVI_ID) "
        + "VALUES (?,?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, serviceInfoId);
      pstmt.setBytes(2, serviceInfo);
      pstmt.setInt(3, serviceInfo.length);
      if (isCborData) {
        pstmt.setString(4, OwnerServiceInfoSequence.CBOR_TYPE);
      } else {
        pstmt.setString(4, OwnerServiceInfoSequence.PLAIN_TYPE);
      }
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Remove Service info value as identified by serviceinfoId from the database.
   * 
   * @param ds Datasource
   * @param serviceInfoId Serviceinfo Identifier
   */
  public void removeServiceInfo(DataSource ds, String serviceInfoId) {
    String sql = "DELETE FROM OWNER_SERVICEINFO WHERE SVI_ID = ?;";
    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, serviceInfoId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Assign svi as given in 'sviString', to the given device. The sviString is off the form
   * 'modName:msgName=serviceinfoId1,modName:msgName=serviceinfoId1....'.
   * 
   * @param ds Datasource
   * @param guid Device GUID
   * @param sviString svi list
   */
  public void assignSviToDevice(DataSource ds, UUID guid, String sviString) {

    String sql = "INSERT INTO GUID_OWNERSVI VALUES (?,?,?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      sviString = sviString.replace("\n", "").replace("\r", "");
      for (String sviEntry : sviString.split(SVI_ARRAY_DELIMETER)) {
        String[] sviEntryArray = sviEntry.split(SVI_ENTRY_DELIMETER);
        String[] modMsgDelimeted = sviEntryArray[0].split(SVI_MODMSG_DELIMETER);

        pstmt.setString(1, guid.toString());
        pstmt.setString(2, sviEntryArray[1]);
        pstmt.setString(3, modMsgDelimeted[0]);
        pstmt.setString(4, modMsgDelimeted[1]);
        // +1 so that the created time is necessarily different for all serviceinfo,
        // this maintains insertion (ascending) order
        Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis() + 1);
        pstmt.setTimestamp(5, created);
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Remove svi for a given device.
   * 
   * @param ds Datasource
   * @param guid Device GUID
   */
  public void removeSviFromDevice(DataSource ds, UUID guid) {
    String sql = "DELETE FROM GUID_OWNERSVI WHERE GUID = ?;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, guid.toString());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load Sample Owner service info from the file-system.
   * 
   * @param ds Datasource instance
   * @param guid Device GUID
   * @param sviValues Path to 'sample-values' directory containing serviceinfo
   * @param svi Path to 'sample-svi.csv' file that contains svi String
   */
  public void loadSampleServiceInfo(DataSource ds, UUID guid, Path sviValues, Path svi) {
    if (sviValues == null || !sviValues.toFile().isDirectory() || svi == null) {
      return;
    }
    try (Stream<Path> files = Files.walk(sviValues, 1)) {
      files.map(Path::toAbsolutePath).filter(Files::isRegularFile).forEach((path) -> {
        try {
          byte[] value = Files.readAllBytes(path);
          addServiceInfo(ds, path.getFileName().toString(), value, false);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      assignSviToDevice(ds, guid, Files.readString(svi));
    } catch (IOException e) {
      System.out.println("Unable to load sample service info");
      throw new RuntimeException(e);
    }
  }

  /**
   * Update the replacementRvInfo for given currentGuid.
   *
   * @param ds Datasource instance
   * @param currentGuid The GUID of the device for which updates need to be made.
   * @param replacementRvInfo The replacement/new RvInfo for the device.
   */
  public void updateDeviceReplacementRvinfo(DataSource ds, UUID currentGuid,
      String replacementRvInfo) {

    String sql = "UPDATE TO2_DEVICES"
        + " SET REPLACEMENT_RVINFO = ?"
        + " WHERE GUID = ?;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setBytes(1, RendezvousInfoDecoder.decode(replacementRvInfo).toBytes());
      pstmt.setString(2, currentGuid.toString());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Update the replacementGuid for given currentGuid.
   *
   * @param ds Datasource instance
   * @param currentGuid The GUID of the device for which updates need to be made.
   * @param replacementGuid The replacement/new GUID for the device.
   */
  public void updateDeviceReplacementGuid(DataSource ds, UUID currentGuid, UUID replacementGuid) {

    String sql = "UPDATE TO2_DEVICES"
        + " SET REPLACEMENT_GUID = ?"
        + " WHERE GUID = ?;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, replacementGuid.toString());
      pstmt.setString(2, currentGuid.toString());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
