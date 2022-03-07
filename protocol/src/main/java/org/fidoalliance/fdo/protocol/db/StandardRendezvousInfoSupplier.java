// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousInfoSupplier;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardRendezvousInfoSupplier implements RendezvousInfoSupplier {

  private static class RootConfig {
    @JsonProperty("manufacturer")
    private RvInfoConfig config;

    protected RvInfoConfig getRoot() {
      return config;
    }
  }

  private static class RvInfoConfig {
    @JsonProperty("rv-instruction")
    private RvInstructionConfig rvInstruction;

    public RvInstructionConfig getRoot() {
      return rvInstruction;
    }
  }

  private static class RvInstructionConfig {
    @JsonProperty("dns")
    private String dns;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("ownerport")
    private String ownerport;

    @JsonProperty("devport")
    private String devport;

    public String getDns() {
      return dns;
    }

    public String getIp() {
      return ip;
    }

    public String getProtocol() {
      return protocol;
    }

    public String getOwnerport() {
      return ownerport;
    }

    public String getDevport() {
      return devport;
    }
  }

  private static final RvInstructionConfig config =
          Config.getConfig(RootConfig.class).getRoot().getRoot();

  @Override
  public RendezvousInfo get() throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      RvData rvData =
          session.find(RvData.class, Long.valueOf(1));

      if (rvData == null) {
        rvData = new RvData();

        final String defaultRvi = "- - - 5\n"
            + "    - \"%s\"\n"
            + "  - - 3\n"
            + "    - %s\n"
            + "  - - 12\n"
            + "    - %s\n"
            + "  - - 2\n"
            + "    - \"%s\"\n"
            + "  - - 4\n"
            + "    - %s";

        String rviString = String.format(defaultRvi, config.getDns(), config.getDevport(),
                config.getProtocol(), config.getIp(), config.getOwnerport());
        RendezvousInfo rvi = Mapper.INSTANCE.readValue(rviString, RendezvousInfo.class);

        rvData.setData(Mapper.INSTANCE.writeValue(rvi));

        session.persist(rvData);
      }
      trans.commit();

      return Mapper.INSTANCE.readValue(rvData.getData(), RendezvousInfo.class);

    } finally {
      session.close();
    }
  }
}
