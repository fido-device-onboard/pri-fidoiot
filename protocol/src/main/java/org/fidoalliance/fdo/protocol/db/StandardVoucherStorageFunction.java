package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.Blob;
import java.util.Date;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.ResellVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardVoucherStorageFunction implements VoucherStorageFunction {

  @Override
  public UUID apply(String serialNo, OwnershipVoucher ownershipVoucher) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ResellVoucher resellVoucher = new ResellVoucher();
      resellVoucher.setId(serialNo);

      OwnershipVoucherHeader header =
          ownershipVoucher.getHeader().unwrap(OwnershipVoucherHeader.class);

      resellVoucher.setCreatedOn(new Date(System.currentTimeMillis()));

      byte[] data = Mapper.INSTANCE.writeValue(ownershipVoucher);
      Transaction trans = session.beginTransaction();

      Blob blob = session.getLobHelper().createBlob(data);
      resellVoucher.setData(blob);
      session.persist(resellVoucher);
      trans.commit();

      return header.getGuid().toUuid();
    } finally {
      session.close();
    }

  }
}
