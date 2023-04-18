// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.Date;
import java.util.List;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;


/**
 * DeviceInfo REST endpoint serves information like {guid, serial no & timestamp } of devices that
 * completed DI.
 *
 * <p>Accepted URL pattern : GET /api/v1/deviceinfo/*
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */
public class DeviceInfo extends RestApi {

  @Override
  public void doGet() throws Exception {

    LoggerService logger = new LoggerService(DeviceInfo.class);

    // Collect pollseconds from the request URL.
    String pollSeconds = getLastSegment();
    int pollTime = 0;

    try {
      pollTime = Integer.parseInt(pollSeconds);
      if (pollTime <= 0) {
        logger.warn("Received poll time below 1 second. Defaulting to 20 seconds");
        pollTime = 20;
      }
    } catch (NumberFormatException e) {
      logger.warn("Received invalid or empty poll time value. Defaulting to 20 seconds");
      pollTime = 20;
    }

    // Retrieve all rows from MANUFACTURED_VOUCHER table.
    CriteriaBuilder builder = getSession().getCriteriaBuilder();
    CriteriaQuery<ManufacturedVoucher> criteria = builder.createQuery(ManufacturedVoucher.class);
    criteria.from(ManufacturedVoucher.class);
    List<ManufacturedVoucher> vouchers = getSession().createQuery(criteria).getResultList();

    // Creating JSON ObjectMapper object
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode rootNode = mapper.createArrayNode();

    for (ManufacturedVoucher voucher : vouchers) {

      // Calculating the time difference between current time and onboarded time.
      Date createdOn = voucher.getCreatedOn();
      Date current = new Date(System.currentTimeMillis());
      long diff = (current.getTime() - createdOn.getTime()) / 1000;

      if (diff <= pollTime) {
        // if time difference is less than polltime. Collect serial no & guid from voucher.
        // append the data to root JSON node.
        ObjectNode obj = mapper.createObjectNode();
        OwnershipVoucher ov = Mapper.INSTANCE.readValue(voucher.getData(), OwnershipVoucher.class);
        OwnershipVoucherHeader header = Mapper.INSTANCE.readValue(ov.getHeader(),
            OwnershipVoucherHeader.class);
        obj.put("serial_no", voucher.getSerialNo());
        obj.put("timestamp", createdOn.toString());
        obj.put("uuid", header.getGuid().toString());
        obj.put("alias", VoucherUtils.getPublicKeyAlias(ov));
        logger.debug("Adding data (serial_no, timestamp, uuid, alias) to root JSON node");
        rootNode.add(obj);
      }
    }

    getResponse().getWriter().write(rootNode.toString());

  }
}
