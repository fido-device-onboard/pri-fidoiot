// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.StandardTo0Client;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.db.OnboardConfigSupplier;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.hibernate.Session;

/**
 * Maintains Ownership Vouchers for the owner server.
 */
public class OwnerVoucher extends RestApi {

  protected static LoggerService logger = new LoggerService(OwnerVoucher.class);

  private void listVouchers() {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {

      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<OnboardingVoucher> cq = cb.createQuery(OnboardingVoucher.class);
      Root<OnboardingVoucher> rootEntry = cq.from(OnboardingVoucher.class);
      CriteriaQuery<OnboardingVoucher> all = cq.select(rootEntry);

      TypedQuery<OnboardingVoucher> allQuery = session.createQuery(all);
      List<OnboardingVoucher> list = allQuery.getResultList();
      OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();
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

  @Override
  public void doPost() throws Exception {

    getTransaction();
    OwnershipVoucher voucher = VoucherUtils.fromString(getStringBody());
    OwnershipVoucherHeader header = Mapper.INSTANCE.readValue(voucher.getHeader(),
        OwnershipVoucherHeader.class);
    byte[] data = Mapper.INSTANCE.writeValue(voucher);
    String guid = header.getGuid().toString();

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class,
        header.getGuid().toString());
    if (onboardingVoucher != null) {

      onboardingVoucher.setGuid(guid.toString());
      onboardingVoucher.setData(data);
      getSession().update(onboardingVoucher);
      getTransaction().commit();
    } else {

      onboardingVoucher = new OnboardingVoucher();
      onboardingVoucher.setGuid(guid.toString());
      onboardingVoucher.setData(data);
      onboardingVoucher.setTo0Expiry(new Date(System.currentTimeMillis()));
      onboardingVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
      getSession().save(onboardingVoucher);
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
      return;
    }

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, path);
    if (onboardingVoucher != null) {

      String text = VoucherUtils.toString(onboardingVoucher.getData());
      getResponse().getWriter().print(text);
    } else {
      throw new NotFoundException(path);
    }
  }

  @Override
  public void doDelete() throws NotFoundException {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect 'guid' from HttpRequest URL last segment
    String guid = getLastSegment();

    // Query database table ONBOARDING_VOUCHER for guid
    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, guid);

    if (onboardingVoucher != null) {
      // delete the row, if data exists.
      getSession().delete(onboardingVoucher);
    } else {
      logger.warn("GUID not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
