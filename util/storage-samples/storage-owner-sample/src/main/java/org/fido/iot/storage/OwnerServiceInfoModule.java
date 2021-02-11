// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.serviceinfo.SdoWget;
import org.fido.iot.serviceinfo.ServiceInfoEntry;
import org.fido.iot.serviceinfo.ServiceInfoModule;
import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class OwnerServiceInfoModule implements ServiceInfoModule {

  private DataSource dataSource;
  private static final String SVI_ARRAY_DELIMETER = ",";
  private static final String SVI_ENTRY_DELIMETER = "=";
  private static final String SVI_MODMSG_DELIMETER = ":";

  public OwnerServiceInfoModule(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // Return List of owner serviceinfo entries parsed by moduleName:messageName and value
  // General idea is that the serviceinfo is fetched in their insertion order. Service info
  // must be inserted into the storage in the exact order (using ascending timestamp)
  // that they are supposed to be transferred.
  @Override
  public List<ServiceInfoEntry> getServiceInfo(UUID uuid) {
    List<ServiceInfoEntry> serviceInfoEntries = new LinkedList<ServiceInfoEntry>();
    String sviString = null;

    // Add logic to identify devicetype

    String deviceType = identifyDeviceType(uuid);

    String sql = "SELECT OWNERSVI_STRING FROM DEVICE_TYPE_OWNERSVI_STRING WHERE DEVICE_TYPE = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, deviceType);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          sviString = rs.getString(1).trim();
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    if (!sviString.equals("null")) {
      sviString = insertModActivateSviEntry(sviString);
      if (checkWgetInSviString(sviString) && isWgetVerificationEnabled()) {
        sviString = insertWgetContentHash(sviString);
      }
      String[] keys = sviString.split(SVI_ARRAY_DELIMETER);
      for (String key : keys) {
        String[] keyElements = key.split(SVI_ENTRY_DELIMETER);
        ServiceInfoSequence valueSequence =
            new OwnerServiceInfoSequence(keyElements[1], dataSource);
        valueSequence.initSequence();
        serviceInfoEntries.add(new ServiceInfoEntry(keyElements[0], valueSequence));
      }
    }
    return serviceInfoEntries;
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
   * @return checks wget verification enable status
   */
  public boolean isWgetVerificationEnabled() {
    boolean isWgetContentVerificationEnabled = false;

    String sql = "SELECT WGET_SVI_MOD_VERIFICATION FROM TO2_SETTINGS WHERE ID = 1";
    try (Connection conn = dataSource.getConnection();
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
   * @param sviString svi mapping string to be updated
   * @return updated sviString with activate module entries
   */
  public String insertWgetContentHash(String sviString) {
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        OwnerDbManager ownerDbManager = new OwnerDbManager();
        ownerDbManager.addServiceInfo(dataSource, sviId, wgetFileContentHash);

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
   * Performs expected value comparision of DSI keys
   * specified for device type identification against the
   * actual value sent by the device.
   *
   * @param uuid UUID
   * @return Device Type
   */
  private String identifyDeviceType(UUID uuid) {

    List<String> deviceTypes = new ArrayList<>();
    List<String> criterias = new ArrayList<>();
    List<String> expectedValues = new ArrayList<>();
    String deviceType = "default";

    String sql = "SELECT * FROM DEVICE_TYPE_OWNERSVI_CRITERIA;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          deviceTypes.add(rs.getString(1));
          criterias.add(rs.getString(2));
          expectedValues.add(rs.getString(3));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    for (int i = 0; i < deviceTypes.size(); i++) {
      String[] criteria = criterias.get(i).split(",");
      String[] expectedValue = expectedValues.get(i).split(",");
      boolean mismatchFound = false;

      if ((criteria.length == expectedValue.length)
          && (checkCriteriaReferentialIntegrity(criteria, uuid))) {
        for (int j = 0; j < criteria.length; j++) {
          sql = "SELECT DSI_VALUE FROM GUID_DEVICEDSI WHERE GUID = ? AND DSI_KEY = ?;";
          try (Connection conn = dataSource.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(uuid));
            pstmt.setString(2, criteria[j]);
            try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                String actualValue = new String(rs.getBytes(1), StandardCharsets.UTF_8);
                if (!expectedValue[j].equals(actualValue.substring(1))) {
                  mismatchFound = true;
                }
              }
            }
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        }

        if (!mismatchFound) {
          deviceType = deviceTypes.get(i);
          break;
        }
      }
    }

    return deviceType;
  }

  /**
   * Verifies if criteria strings matches with the DSI keys sent by the device.
   *
   * @param criteria list of device svi to be used for identifying device types
   * @param uuid UUID
   * @return true or false
   */
  private boolean checkCriteriaReferentialIntegrity(String[] criteria, UUID uuid) {
    ArrayList<String> dsiKeys = new ArrayList<>();
    String sql = "SELECT * FROM GUID_DEVICEDSI WHERE GUID = ?;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, uuid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {

        while (rs.next()) {
          dsiKeys.add(rs.getString(2));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (dsiKeys.containsAll(Arrays.asList(criteria))) {
      return true;
    }
    return false;
  }

  @Override
  public void putServiceInfo(UUID uuid, ServiceInfoEntry entry) {
    String sql = "MERGE INTO GUID_DEVICEDSI KEY (GUID, DSI_KEY) VALUES(?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, uuid.toString());
      pstmt.setString(2, entry.getKey());
      pstmt.setBytes(3, (byte[]) entry.getValue().getContent());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
