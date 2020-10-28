package org.fido.iot.storage;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import javax.xml.bind.DatatypeConverter;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidGuidException;
import org.fido.iot.protocol.MessageBodyException;
import org.fido.iot.protocol.ResourceNotFoundException;

public class To0AllowListDenyListDbStorage extends To0DbStorage {

  /**
   * Constructs a To0DbStorage instance.
   *
   * @param cryptoService A crypto Service.
   * @param dataSource A SQL datasource.
   */
  public To0AllowListDenyListDbStorage(CryptoService cryptoService, DataSource dataSource) {
    super(cryptoService, dataSource);
  }

  /**
   * Stores Redirect Blob in the DB.
   *
   * @param voucher ownership voucher
   * @param requestedWait waitsecond
   * @param signedBlob signed Blob
   * @return waitseconds for the GUID
   */
  public long storeRedirectBlob(Composite voucher, long requestedWait, byte[] signedBlob) {

    checkGuidAgainstDenyList(
        DatatypeConverter.printHexBinary(
            voucher.getAsComposite(Const.OV_HEADER).getAsBytes(Const.OVH_GUID)));

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);

    Composite encodedKey = getCryptoService().getOwnerPublicKey(voucher);
    PublicKey pubKey = getCryptoService().decode(encodedKey);
    String ownerX509String = getCryptoService().getFingerPrint(pubKey);
    pubKey = getCryptoService().getDevicePublicKey(voucher);
    Composite deviceX509 = getCryptoService().encode(pubKey, Const.PK_ENC_X509);

    String sql = "" + "MERGE INTO RV_REDIRECTS  " + "KEY (GUID) " + "VALUES (?,?,?,?,?,?,?); ";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      pstmt.setBytes(2, signedBlob);
      pstmt.setString(3, ownerX509String);
      pstmt.setBytes(4, deviceX509.toBytes());
      pstmt.setInt(5, Long.valueOf(requestedWait).intValue());
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(6, created);
      Timestamp expiresAt = new Timestamp(Calendar.getInstance().getTimeInMillis() + requestedWait);
      pstmt.setTimestamp(7, expiresAt);

      pstmt.executeUpdate();

      sql = "SELECT WAIT_SECONDS_RESPONSE FROM RV_REDIRECTS WHERE GUID = ?";
      try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {

        pstmt2.setString(1, guid.toString());
        try (ResultSet rs = pstmt2.executeQuery()) {
          while (rs.next()) {
            return rs.getInt(1);
          }
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    throw new ResourceNotFoundException(guid.toString());
  }

  /**
   * Check GUID against GUID_DENYLIST table entries.
   *
   * @param guid Device GUID
   */
  public void checkGuidAgainstDenyList(String guid) {

    String sql = "SELECT * FROM GUID_DENYLIST WHERE GUID = ?";

    int rowCount = 0;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, guid);
      try (ResultSet rs = pstmt.executeQuery()) {
        rs.last();
        rowCount = rs.getRow();
      }
    } catch (SQLException sqlException) {
      throw new RuntimeException(sqlException);
    }

    if (rowCount > 0) {
      throw new InvalidGuidException(new MessageBodyException());
    }
  }
}
