package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;

public class AioApp {

  public static void main(String args[]) {
    //HibernateUtil.getSessionFactory();
    Config.getWorker(HttpServer.class).run();
  }
}
