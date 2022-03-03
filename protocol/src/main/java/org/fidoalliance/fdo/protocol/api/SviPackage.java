package org.fidoalliance.fdo.protocol.api;


import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSysInstruction;
import org.fidoalliance.fdo.protocol.entity.SystemPackage;
import org.hibernate.engine.jdbc.ClobProxy;

public class SviPackage extends RestApi {

  @Override
  public void doGet() throws Exception {
    getTransaction();

    SystemPackage systemPackage =
        getSession().find(SystemPackage.class,Long.valueOf(1));

    if (systemPackage != null) {
      String result = systemPackage.getData().getSubString(1,
          Long.valueOf(systemPackage.getData().length()).intValue());
      getResponse().getWriter().print(result);
    }
  }
  @Override
  public void doPost() throws Exception {
    String body = getStringBody();
    getTransaction();

    FdoSysInstruction[] instructions = Mapper.INSTANCE.readJsonValue(body, FdoSysInstruction[].class);

    SystemPackage systemPackage =
        getSession().find(SystemPackage.class,Long.valueOf(1));

    if (systemPackage == null) {
      systemPackage = new SystemPackage();
    }
    systemPackage.setData(ClobProxy.generateProxy(body));
    getSession().persist(systemPackage);
  }
}
