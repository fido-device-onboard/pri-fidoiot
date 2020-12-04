// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
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
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.VoucherExtensionService;

/**
 * The Reseller voucher servlet.
 */
public class ResellerVoucherServlet extends HttpServlet {

  protected Composite queryVoucher(DataSource dataSource, String serialNo) {

    String sql = "SELECT RT_DEVICES.VOUCHER, RT_CUSTOMERS.KEYS "
        + "FROM RT_DEVICES "
        + "LEFT JOIN RT_CUSTOMERS "
        + "ON RT_CUSTOMERS.CUSTOMER_ID=RT_DEVICES.CUSTOMER_ID "
        + "WHERE RT_DEVICES.SERIAL_NO = ?";
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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String uri = req.getRequestURI();
    String id = req.getParameter("id");
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);
    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    KeyResolver resolver = (KeyResolver) getServletContext().getAttribute("keyresolver");

    if (id != null) {
      new ResellerDbManager().assignCustomer(ds, serialNo, id);
    }

    Composite result = queryVoucher(ds, serialNo);
    if (result.size() == 0) {
      resp.setStatus(401);
      return;
    }

    Composite voucher = result.getAsComposite(Const.FIRST_KEY);
    List<PublicKey> nextOwnerPublicKeys =
        PemLoader.loadPublicKeys(result.getAsString(Const.SECOND_KEY));

    //find the public key
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    Composite prevOwner = ovh.getAsComposite(Const.OVH_PUB_KEY);
    Composite entries = voucher.getAsComposite(Const.OV_ENTRIES);
    //can be zero
    if (entries.size() > 0) {
      Composite entry = entries.getAsComposite(entries.size() - 1);
      Composite payload = Composite.fromObject(entry.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
      prevOwner = payload.getAsComposite(Const.OVE_PUB_KEY);
    }

    CryptoService cs = (CryptoService) getServletContext().getAttribute("cryptoservice");
    PublicKey ownerPub = cs.decode(prevOwner);
    PublicKey nextOwner = null;
    int keyType = prevOwner.getAsNumber(Const.PK_TYPE).intValue();
    for (PublicKey pub : nextOwnerPublicKeys) {
      int ownerType = cs.getPublicKeyType(pub);
      if (ownerType == keyType) {
        nextOwner = pub;
        break;
      }
    }
    // we didn't find an owner entry in database as per the current owner's key-type.
    if (null == nextOwner) {
      System.out.println("Customer entry missing for " + serialNo);
      resp.setStatus(500);
      return;
    }

    VoucherExtensionService vse = new VoucherExtensionService(voucher, cs);
    try (CloseableKey signer = new CloseableKey(resolver.getKey(ownerPub))) {
      if (signer.get() != null) {
        vse.add(signer.get(), nextOwner);
      } else {
        System.out.println("Reseller is not the current owner for " + serialNo);
        resp.setStatus(500);
        return;
      }
    }

    resp.setContentType(Const.HTTP_APPLICATION_CBOR);
    byte[] voucherBytes = voucher.toBytes();
    getServletContext().log("Extended voucher: " + Composite.toString(voucherBytes));
    resp.setContentLength(voucherBytes.length);
    resp.getOutputStream().write(voucherBytes);

  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase(Const.HTTP_APPLICATION_CBOR) != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    String uri = req.getRequestURI();
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);
    String id = req.getParameter("id");
    Composite voucher = Composite.fromObject(req.getInputStream());
    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    new ResellerDbManager().importVoucher(ds, voucher, serialNo, id);
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String uri = req.getRequestURI();
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);
    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    new ResellerDbManager().deleteVoucher(ds, serialNo);
  }
}
