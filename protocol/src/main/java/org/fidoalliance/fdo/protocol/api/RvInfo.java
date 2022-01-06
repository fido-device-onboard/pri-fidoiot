package org.fidoalliance.fdo.protocol.api;

import java.sql.Blob;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.hibernate.Transaction;

public class RvInfo extends RestApi {

  @Override
  public void doPost() throws Exception {

    String body = getStringBody();

    RendezvousInfo info =
          Mapper.INSTANCE.readValue(body, RendezvousInfo.class);

    byte[] data = Mapper.INSTANCE.writeValue(info);
    getTransaction();
    Blob blob = getSession().getLobHelper().createBlob(data);


    RvData rviData =
        getSession().get(RvData.class,Long.valueOf(1));

    if (rviData == null) {
      rviData = new RvData();
      rviData.setId(Long.valueOf(1));

      rviData.setData(blob);
      getSession().persist(rviData);

    } else {
      getSession().update(rviData);
    }
  }
}
