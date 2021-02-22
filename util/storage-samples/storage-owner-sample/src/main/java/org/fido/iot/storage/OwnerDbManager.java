// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.serviceinfo.SdoWget;

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
          + "OWNER_SERVICE_INFO_MTU_SIZE INT NULL DEFAULT NULL, "
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
          + "SERVICEINFO_BLOB BLOB, "
          + "CREATED TIMESTAMP,"
          + "UPDATED TIMESTAMP,"
          + "PRIMARY KEY (SESSION_ID), "
          + "UNIQUE (SESSION_ID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
              + "TO2_SETTINGS("
              + "ID INT NOT NULL, "
              + "DEVICE_SERVICE_INFO_MTU_SIZE INT NOT NULL, "
              + "OWNER_MTU_THRESHOLD INT NOT NULL, "
              + "WGET_SVI_MOD_VERIFICATION BOOLEAN NOT NULL, "
              + "PRIMARY KEY (ID), "
              + "UNIQUE (ID)"
              + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "OWNER_SERVICEINFO("
          + "SVI_ID CHAR(36) PRIMARY KEY, "
          + "CONTENT BLOB, "
          + "CONTENT_LENGTH BIGINT, "
          + "PRIMARY KEY (SVI_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "GUID_OWNERSVI("
          + "ID INT NOT NULL AUTO_INCREMENT,"
          + "GUID CHAR(36), "
          + "SVI_ID CHAR(36), "
          + "MODULE_NAME CHAR(36), "
          + "MESSAGE_NAME CHAR(36),"
          + "CREATED_AT TIMESTAMP, "
          + "PRIMARY KEY (ID, GUID, SVI_ID),"
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
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?); ";

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
      pstmt.setInt(14, 0);

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
   */
  public void addServiceInfo(DataSource ds, String serviceInfoId, byte[] serviceInfo) {
    String sql = ""
        + "MERGE INTO OWNER_SERVICEINFO  "
        + "KEY (SVI_ID) "
        + "VALUES (?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, serviceInfoId);
      pstmt.setBytes(2, serviceInfo);
      pstmt.setInt(3, serviceInfo.length);
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

    sviString = insertModActivateSviEntry(sviString);

    if (checkWgetInSviString(sviString) && isWgetVerificationEnabled(ds)) {
      sviString = insertWgetContentHash(ds, sviString);
    }

    String sql = "INSERT INTO GUID_OWNERSVI "
            + "(GUID, SVI_ID, MODULE_NAME, MESSAGE_NAME, CREATED_AT) "
            + "VALUES (?,?,?,?,?); ";

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
   * For every module change in sviString, inserts mod:active SVI entry.
   *
   * @param sviString svi mapping string to be updated
   * @return updated sviString with activate module entries
   */
  public String insertModActivateSviEntry(String sviString) {
    StringBuilder sb = new StringBuilder();
    List<String> sviEntries = Arrays.asList(sviString.split(","));
    List<String> modSequence = new ArrayList<>();

    for (String sviEntry : sviEntries) {
      String[] sviEntryArray = sviEntry.split(SVI_ENTRY_DELIMETER);
      String[] modMsgDelimeted = sviEntryArray[0].split(SVI_MODMSG_DELIMETER);
      modSequence.add(modMsgDelimeted[0]);
    }

    sb.append(modSequence.get(0));
    sb.append(":active=activate_mod,");
    sb.append(sviEntries.get(0) + ",");
    for (int i = 1; i < modSequence.size(); i++) {
      if (!(modSequence.get(i - 1).equals(modSequence.get(i)))) {
        sb.append(modSequence.get(i));
        sb.append(":active=activate_mod,");
      }
      sb.append(sviEntries.get(i) + ",");
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  /**
   * Determines if wget module is included in svi.
   *
   * @param sviString svi mapping string
   * @return checks if wget module entries are present in the service info string
   */
  public boolean checkWgetInSviString(String sviString) {

    List<String> sviEntries = Arrays.asList(sviString.split(","));

    for (String sviEntry : sviEntries) {
      String[] sviEntryArray = sviEntry.split(SVI_ENTRY_DELIMETER);
      String[] modMsgDelimeted = sviEntryArray[0].split(SVI_MODMSG_DELIMETER);
      if (modMsgDelimeted[0].equals(SdoWget.NAME)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if content verification is enabled for wget file content.
   *
   * @param ds Datasource
   * @return checks wget verification enable status
   */
  public boolean isWgetVerificationEnabled(DataSource ds) {
    boolean isWgetContentVerificationEnabled = false;

    String sql = "SELECT WGET_SVI_MOD_VERIFICATION FROM TO2_SETTINGS WHERE ID = 1";
    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          isWgetContentVerificationEnabled = rs.getBoolean(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return isWgetContentVerificationEnabled;
  }

  /**
   * Calculated sha384 hash of file content to be transferred using wget:url. Adds the hash entry to
   * OWNER_SERVICEINFO table Includes appropriate mapping in the sviString for corresponding hash
   * file.
   *
   * @param ds datasource
   * @param sviString svi mapping string to be updated
   * @return updated sviString with activate module entries
   */
  public String insertWgetContentHash(DataSource ds, String sviString) {
    StringBuilder sb = new StringBuilder();
    List<String> sviEntries = Arrays.asList(sviString.split(","));
    List<String> newSviEntries = new ArrayList<>();

    for (String sviEntry : sviEntries) {
      String[] sviEntryArray = sviEntry.split(SVI_ENTRY_DELIMETER);

      // Get the filename whose hash needs to be calculated.
      if (sviEntryArray[0]
          .trim()
          .equals(SdoWget.NAME + SVI_MODMSG_DELIMETER + SdoWget.KEY_FILENAME)) {
        String filename = null;
        String sviId = null;
        byte[] sviContent = null;

        String sql = "SELECT CONTENT FROM OWNER_SERVICEINFO WHERE SVI_ID = ?";
        try (Connection conn = ds.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
          pstmt.setString(1, sviEntryArray[1]);
          try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
              filename = new String(rs.getBytes(1), StandardCharsets.UTF_8);
              // concatenate the filename and the hash to generate a unique SVI_ID.
              sviId = new StringBuilder(filename + "_hash").toString();
            }
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        // Read the file content whose hash needs to be calculated.
        sql = "SELECT CONTENT FROM OWNER_SERVICEINFO WHERE SVI_ID = ?";
        try (Connection conn = ds.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
          pstmt.setString(1, filename);
          try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
              sviContent = rs.getBytes(1);
            }
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        // calculate SHA384 hash of the file content.
        byte[] wgetFileContentHash =
            (new CryptoService().hash(Const.SHA_384, sviContent)).getAsBytes(Const.HASH);

        // Adds the hash entry to OWNER_SERVICEINFO table.
        addServiceInfo(ds, sviId, wgetFileContentHash);

        // add the mapping to GUID_OWNERSVI
        newSviEntries.add(
            (SdoWget.NAME + SVI_MODMSG_DELIMETER + SdoWget.KEY_SHA + SVI_ENTRY_DELIMETER + sviId));
      }
    }

    int i = 0;
    for (String sviEntry : sviEntries) {
      sb.append(sviEntry + ",");
      String[] sviEntryArray = sviEntry.split(SVI_ENTRY_DELIMETER);
      if (sviEntryArray[0].trim().equals(SdoWget.NAME + SVI_MODMSG_DELIMETER + SdoWget.KEY_URL)) {
        sb.append(newSviEntries.get(i) + ",");
        i++;
      }
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
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
          addServiceInfo(ds, path.getFileName().toString(), value);
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

  /**
   * Load default value of maximum serviceinfo that owner can receive.
   *
   * @param ds Datasource instance
   */
  public void loadTo2Settings(DataSource ds) {

    String sql = "MERGE INTO TO2_SETTINGS ("
        + "ID,"
        + "DEVICE_SERVICE_INFO_MTU_SIZE, "
        + "OWNER_MTU_THRESHOLD, "
        + "WGET_SVI_MOD_VERIFICATION ) "
        + "VALUES (1,1300,8192,FALSE);";

    try (Connection conn = ds.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Update the given field with the required MTU size in TO2_SETTINGS table.
   *
   * @param ds Datasource instance
   * @param field Database column name
   * @param mtu maximum MTU size
   */
  public void updateMtu(DataSource ds, String field, int mtu) {

    String sql = "UPDATE TO2_SETTINGS" + " SET " + field + " = ?" + " WHERE ID = 1;";

    if (mtu > 0 && mtu < Const.SERVICE_INFO_MTU_MIN_SIZE) {
      System.out.println(
          "Received value less than default minimum of 256 bytes. "
              + "Updating MTU size to default minimum");
      mtu = Const.SERVICE_INFO_MTU_MIN_SIZE;
    }

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, String.valueOf(mtu));
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates content verification preference for wget module.
   *
   * @param ds Datasource instance
   * @param contentVerificationPreference true or false
   */
  public void updateWgetVerificationPreference(
      DataSource ds, Boolean contentVerificationPreference) {

    String sql = "UPDATE TO2_SETTINGS SET WGET_SVI_MOD_VERIFICATION = ? WHERE ID = 1;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, String.valueOf(contentVerificationPreference));
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
