package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class MfgVoucher extends RestApi {


  @Override
  public void doPost() throws Exception {

    String serialNo = getLastSegment();

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, serialNo);
    if (mfgVoucher == null) {
      throw new NotFoundException(serialNo);
    }
    OwnershipVoucher voucher = Mapper.INSTANCE.readValue(mfgVoucher.getData(),
        OwnershipVoucher.class);

    KeyResolver resolver = Config.getWorker(ManufacturerKeySupplier.class).get();

    List<Certificate> list = PemLoader.loadCerts(getStringBody());
    Certificate[] certs = list.stream()
        .toArray(Certificate[]::new);

    OwnershipVoucher result = VoucherUtils.extend(voucher, resolver, certs);
    byte[] data = Mapper.INSTANCE.writeValue(result);

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(VoucherUtils.toString(data));
  }

  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, path);
    if (mfgVoucher == null) {
      throw new NotFoundException(path);
    }
    String text = VoucherUtils.toString(mfgVoucher.getData());
    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(text);
  }
}
