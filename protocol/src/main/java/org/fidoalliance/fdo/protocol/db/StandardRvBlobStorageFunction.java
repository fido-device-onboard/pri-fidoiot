package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.RvBlobStorageFunction;
import org.fidoalliance.fdo.protocol.entity.RvRedirect;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardRvBlobStorageFunction implements RvBlobStorageFunction {

  @Override
  public Long apply(To0d to0d, CoseSign1 coseSign1) throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      //todo: get wait seconds
      long waitSeconds = to0d.getWaitSeconds();

      //todo: test owner public key and guid deniy list

      final OwnershipVoucherHeader header = to0d.getVoucher().getHeader()
          .unwrap(OwnershipVoucherHeader.class);

      final byte[] data = Mapper.INSTANCE.writeValue(coseSign1);
      RvRedirect redirect = session.get(RvRedirect.class, header.getGuid().toString());
      if (redirect == null) {
        redirect = new RvRedirect();
        redirect.setGuid(header.getGuid().toString());
        redirect.setData(data);
        redirect.setExpiry(
            new Date(System.currentTimeMillis()
                + Duration.ofSeconds(waitSeconds).toMillis()));
        redirect.setCreatedOn(new Date(System.currentTimeMillis()));

        session.save(redirect);
      }
      else {
        redirect.setData(data);
        redirect.setExpiry(
                new Date(System.currentTimeMillis()
                    + Duration.ofSeconds(waitSeconds).toMillis()));
        session.update(redirect);
      }
      trans.commit();

      return waitSeconds;

    } finally {
      session.close();
    }
  }
}
