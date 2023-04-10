// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.List;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementKeySupplier;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

/**
 * Performs resell operation.
 */
public class ResellApi extends RestApi {
  protected static final LoggerService logger = new LoggerService(ResellApi.class);

  @Override
  public void doPost() throws Exception {

    String body = getStringBody();
    OwnershipVoucher voucher = null;
    String path = getLastSegment();
    KeyResolver resolver = Config.getWorker(OwnerKeySupplier.class).get();
    if (path.indexOf("-") > 0) {
      OnboardingVoucher onboardingVoucher = getSession().find(OnboardingVoucher.class, path);
      if (onboardingVoucher != null) {

        if (onboardingVoucher.getReplacement() != null) {
          voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getReplacement(),
              OwnershipVoucher.class);

          resolver = Config.getWorker(ReplacementKeySupplier.class).get();
        } else {
          voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getData(), OwnershipVoucher.class);

        }

      } else {
        logger.warn("Voucher not found for GUID: " + path);
        throw new NotFoundException(path);
      }
    } else {
      voucher = VoucherUtils.fromString(body);
    }

    List<Certificate> list = PemLoader.loadCerts(body);
    Certificate[] certs = list.stream()
        .toArray(Certificate[]::new);

    OwnershipVoucher result = VoucherUtils.extend(voucher, resolver, certs);
    byte[] data = Mapper.INSTANCE.writeValue(result);

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(VoucherUtils.toString(data));
  }

}