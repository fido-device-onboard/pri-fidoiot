// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.entity.VoucherAlias;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;

/**
 * Maintains Ownership Vouchers for the owner server.
 */
public class OwnerVoucher extends RestApi {

  protected static final LoggerService logger = new LoggerService(OwnerVoucher.class);

  private void listVouchers() {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {

      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<OnboardingVoucher> cq = cb.createQuery(OnboardingVoucher.class);
      Root<OnboardingVoucher> rootEntry = cq.from(OnboardingVoucher.class);
      CriteriaQuery<OnboardingVoucher> all = cq.select(rootEntry);

      TypedQuery<OnboardingVoucher> allQuery = session.createQuery(all);
      List<OnboardingVoucher> list = allQuery.getResultList();
      for (OnboardingVoucher onboardingVoucher : list) {

        OwnershipVoucher voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getData(),
            OwnershipVoucher.class);

        OwnershipVoucherHeader header =
            Mapper.INSTANCE.readValue(voucher.getHeader(), OwnershipVoucherHeader.class);

        getResponse().getWriter().println(header.getGuid().toString());

      }

    } catch (IOException e) {
      logger.error("error retrieving vouchers " + e.getMessage());
    } finally {
      session.close();
    }
  }

  private void listSerialNo() {

    try {

      CriteriaBuilder cb = getSession().getCriteriaBuilder();
      CriteriaQuery<VoucherAlias> cq = cb.createQuery(VoucherAlias.class);
      Root<VoucherAlias> rootEntry = cq.from(VoucherAlias.class);
      CriteriaQuery<VoucherAlias> all = cq.select(rootEntry);

      TypedQuery<VoucherAlias> allQuery = getSession().createQuery(all);
      List<VoucherAlias> list = allQuery.getResultList();
      for (VoucherAlias voucherAlias : list) {
        getResponse().getWriter().println(voucherAlias.getAlias());
      }

    } catch (IOException e) {
      logger.error("error retrieving vouchers " + e.getMessage());
    }
  }

  private boolean isGuid(String value) {
    try {
      UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return true;
  }

  @Override
  public void doPost() throws Exception {

    getTransaction();
    OwnershipVoucher voucher = VoucherUtils.fromString(getStringBody());
    OwnershipVoucherHeader header = Mapper.INSTANCE.readValue(voucher.getHeader(),
        OwnershipVoucherHeader.class);
    byte[] data = Mapper.INSTANCE.writeValue(voucher);
    String guid = header.getGuid().toString();
    logger.info("GUID is " + guid);

    String path = getLastSegment();
    if (!path.equals("vouchers")) {

      VoucherAlias voucherAlias = getSession().get(VoucherAlias.class, path);
      if (voucherAlias == null) {
        voucherAlias = new VoucherAlias();
        voucherAlias.setAlias(path);
        voucherAlias.setGuid(guid);
        getSession().persist(voucherAlias);
      } else {
        voucherAlias.setGuid(guid);
        getSession().merge(voucherAlias);
      }
    }

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class,
        header.getGuid().toString());
    if (onboardingVoucher != null) {

      onboardingVoucher.setGuid(guid);
      onboardingVoucher.setData(data);
      getSession().merge(onboardingVoucher);
      getTransaction().commit();
    } else {

      onboardingVoucher = new OnboardingVoucher();
      onboardingVoucher.setGuid(guid);
      onboardingVoucher.setData(data);
      onboardingVoucher.setTo0Expiry(new Date(System.currentTimeMillis()));
      onboardingVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
      getSession().persist(onboardingVoucher);
      getTransaction().commit();
    }
    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(header.getGuid().toString());
  }

  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);

    if (path.equals("vouchers")) {
      listVouchers();
      listSerialNo();
      return;
    }

    //if last segment is serialno vs guid
    if (!isGuid(path)) {
      VoucherAlias voucherAlias = getSession().get(VoucherAlias.class, path);
      if (voucherAlias != null) {
        path = voucherAlias.getGuid();
      }
    }

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, path);
    if (onboardingVoucher != null) {

      String text = VoucherUtils.toString(onboardingVoucher.getData());
      getResponse().getWriter().print(text);
    } else {
      logger.error("Voucher not found for GUID: " + path);
      throw new NotFoundException(path);
    }
  }

  @Override
  public void doDelete() throws NotFoundException {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect 'guid' from HttpRequest URL last segment
    String path = getLastSegment();

    if (!isGuid(path)) {
      VoucherAlias voucherAlias = getSession().get(VoucherAlias.class, path);
      if (voucherAlias != null) {
        path = voucherAlias.getGuid();
        getSession().remove(voucherAlias);
      }
    }

    // Query database table ONBOARDING_VOUCHER for guid
    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, path);

    if (onboardingVoucher != null) {
      // delete the row, if data exists.
      getSession().remove(onboardingVoucher);
    } else {
      logger.warn("GUID not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
