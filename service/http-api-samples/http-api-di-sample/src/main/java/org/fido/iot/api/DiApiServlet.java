// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.CloseableKey;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.VoucherExtensionService;
import org.fido.iot.storage.CertificateResolver;

/**
 * Device Initialization API servlet..
 */
public class DiApiServlet extends HttpServlet {

  /**
   * Returns the keyType of the voucher and it's assigned owner public key.
   * @param dataSource database source
   * @param serialNo serialNo to identify specific voucher
   * @return Composite where first key is the voucher and second is the public keys
   */
  protected Composite queryVoucherAndPublicKeys(DataSource dataSource, String serialNo) {

    String sql = "SELECT MT_DEVICES.VOUCHER, MT_CUSTOMERS.KEYS "
        + "FROM MT_DEVICES "
        + "LEFT JOIN MT_CUSTOMERS "
        + "ON MT_CUSTOMERS.CUSTOMER_ID=MT_DEVICES.CUSTOMER_ID "
        + "WHERE MT_DEVICES.SERIAL_NO = ?";
    Composite result = Composite.newArray();

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, serialNo);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          result = Composite.newArray();
          result.set(Const.FIRST_KEY, Composite.fromObject(rs.getBytes(1)));
          result.set(Const.SECOND_KEY, rs.getString(2));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  protected PublicKey getOwnerPublicKey(int keyType, List<PublicKey> publicKeys) {
    CryptoService cs = (CryptoService) getServletContext().getAttribute("cryptoservice");
    for (PublicKey key : publicKeys) {
      if (key instanceof ECKey) {
        int bitLength = ((ECKey) key).getParams().getCurve().getField().getFieldSize();
        if (keyType == Const.PK_SECP256R1 && bitLength == Const.BIT_LEN_256) {
          return key;
        }
        if (keyType == Const.PK_SECP384R1 && bitLength == Const.BIT_LEN_384) {
          return key;
        }
      } else if ((keyType == Const.PK_RSA || keyType == Const.PK_RSA2048RESTR)
              && key instanceof RSAKey) {
        return key;
      }
    }
    return null;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String uri = req.getRequestURI();
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);

    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    CryptoService cs = (CryptoService) getServletContext().getAttribute("cryptoservice");
    CertificateResolver resolver =
        (CertificateResolver) getServletContext().getAttribute("resolver");

    // get voucher and assigned public keys
    Composite result = queryVoucherAndPublicKeys(ds, serialNo);
    if (result.size() == 0) {
      resp.setStatus(401);
      return;
    }

    Composite voucher = result.getAsComposite(Const.FIRST_KEY);
    List<PublicKey> publicKeys =
        PemLoader.loadPublicKeys(result.getAsString(Const.SECOND_KEY));

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    Composite mfgPub = ovh.getAsComposite(Const.OVH_PUB_KEY);

    int keyType = mfgPub.getAsNumber(Const.PK_TYPE).intValue();
    PublicKey ownerPub = getOwnerPublicKey(keyType, publicKeys);

    Certificate[] issuerChain = resolver.getCertChain(keyType);

    VoucherExtensionService vse = new VoucherExtensionService(voucher, cs);
    try (CloseableKey signer = resolver.getPrivateKey(issuerChain[0])) {
      vse.add(signer.get(), ownerPub);
    }

    resp.setContentType(Const.HTTP_APPLICATION_CBOR);
    byte[] voucherBytes = voucher.toBytes();
    getServletContext().log("Extended voucher: " + Composite.toString(voucherBytes));
    resp.setContentLength(voucherBytes.length);
    resp.getOutputStream().write(voucherBytes);
  }
}
