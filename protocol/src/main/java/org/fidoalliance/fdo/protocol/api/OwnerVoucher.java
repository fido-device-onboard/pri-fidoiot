package org.fidoalliance.fdo.protocol.api;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.StandardTo0Client;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.db.OnboardConfigSupplier;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

public class OwnerVoucher extends RestApi {


  @Override
  public void doPost() throws Exception {

    OwnershipVoucher voucher = VoucherUtils.fromString(getStringBody());
    OwnershipVoucherHeader header = Mapper.INSTANCE.readValue(voucher.getHeader(),
        OwnershipVoucherHeader.class);
    byte[] data = Mapper.INSTANCE.writeValue(voucher);

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class,
        header.getGuid().toString());
    if (onboardingVoucher != null) {
      VoucherUtils.fromString(getStringBody());
      onboardingVoucher.setData(data);
      getSession().update(onboardingVoucher);
    } else {
      VoucherUtils.fromString(getStringBody());
      onboardingVoucher = new OnboardingVoucher();
      onboardingVoucher.setData(data);
      onboardingVoucher.setTo0Expiry(new Date(System.currentTimeMillis()));
      onboardingVoucher.setCreatedOn(new Date(System.currentTimeMillis()));
      getSession().save(onboardingVoucher);
    }
    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(header.getGuid().toString());
  }

  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class,path);
    if (onboardingVoucher != null) {

      String text = VoucherUtils.toString(onboardingVoucher.getData());
      getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
      getResponse().getWriter().print(text);
    }


  }
}
