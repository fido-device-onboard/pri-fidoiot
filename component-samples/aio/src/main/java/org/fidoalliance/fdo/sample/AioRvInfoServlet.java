// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;


public class AioRvInfoServlet extends HttpServlet {

  LoggerService logger = new LoggerService(AioRvInfoServlet.class);

  private static byte[] getIpAddress(String value) {
    try {
      return InetAddress.getByName(value).getAddress();
    } catch (UnknownHostException e) {
      throw new DispatchException(e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    final AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(0);
    new Thread(() -> postAsync(asyncCtx)).start();
  }

  private void postAsync(AsyncContext asyncCtx) {
    HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    HttpServletResponse res = (HttpServletResponse) asyncCtx.getResponse();

    Composite rvInstruction = Composite.newArray();

    final String dns = req.getParameter("dns");
    if (dns != null) {
      rvInstruction.set(rvInstruction.size(), new Object[]{Const.RV_DNS, dns});
    }

    final String ip = req.getParameter("ip");
    if (ip != null) {
      try {
        rvInstruction.set(rvInstruction.size(),
                new Object[]{Const.RV_IP_ADDRESS, getIpAddress(ip)});
      } catch (RuntimeException e) {
        logger.error("Invalid IP address");
      }
    }

    final String rvProt = null != req.getParameter("rvprot")
            ? req.getParameter("rvprot") : "https";
    String devPort = "8443";
    if (rvProt != null) {
      if (rvProt.equals("http")) {
        rvInstruction.set(rvInstruction.size(),
                new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_HTTP});
        devPort = AioConfigLoader.loadConfig(AioAppSettings.AIO_PORT);
      } else if (rvProt.equals("https")) {
        rvInstruction.set(rvInstruction.size(),
                new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_HTTPS});
        devPort = AioConfigLoader.loadConfig(AioAppSettings.AIO_HTTPS_PORT);
      }
    }

    final String ownerPort = AioConfigLoader.loadConfig(AioAppSettings.AIO_HTTPS_PORT);

    rvInstruction.set(rvInstruction.size(),
            new Object[]{Const.RV_DEV_PORT, Integer.parseInt(devPort)});
    rvInstruction.set(rvInstruction.size(),
            new Object[]{Const.RV_OWNER_PORT, Integer.parseInt(ownerPort)});

    Composite rvInfoBlob = Composite.newArray();
    rvInfoBlob.set(rvInfoBlob.size(), rvInstruction);

    List<String> directives = RendezvousInfoDecoder
            .getHttpDirectives(rvInfoBlob, Const.RV_DEV_ONLY);
    if (directives.size() > 0) {
      StringBuilder to0RvBlob = new StringBuilder();
      to0RvBlob.append(rvProt + "://");
      if (dns != null) {
        to0RvBlob.append(dns + ":" + devPort);
        if (ip != null) {
          to0RvBlob.append("?ipaddress=" + ip);
        }
      } else if (ip != null) {
        to0RvBlob.append(ip + ":" + devPort);
      }

      AioDbManager db = new AioDbManager();
      ServletContext sc = req.getServletContext();
      BasicDataSource ds = (BasicDataSource) sc.getAttribute("datasource");
      db.addRvInfo(ds, rvInfoBlob.toString());
      db.updateTo0RvBlob(ds, to0RvBlob.toString());
      logger.info("Updated RVInfo: " + directives.toString());
      res.setStatus(HttpServletResponse.SC_OK);
    } else {
      logger.error("Received invalid RVInfo.");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
    asyncCtx.complete();
  }
}
