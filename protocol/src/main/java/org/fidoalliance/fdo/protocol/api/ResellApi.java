package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.List;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

public class ResellApi extends RestApi {

  @Override
  public void doPost() throws Exception {

    String body = getStringBody();
    OwnershipVoucher voucher = VoucherUtils.fromString(body);

    KeyResolver resolver = Config.getWorker(OwnerKeySupplier.class).get();

    List<Certificate> list = PemLoader.loadCerts(body);
    Certificate[] certs = list.stream()
        .toArray(Certificate[]::new);

    OwnershipVoucher result = VoucherUtils.extend(voucher, resolver, certs);
    byte[] data = Mapper.INSTANCE.writeValue(result);

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(VoucherUtils.toString(data));
  }

}