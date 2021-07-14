// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AioInfoServlet serves Device information that completed DI.
 */
public class AioInfoServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    final AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(0);
    new Thread(() -> getAsync(asyncCtx)).start();
  }

  protected String getPathName(String path) {
    String result = "";
    int pos = path.lastIndexOf('/');
    if (pos >= 0) {
      result = path.substring(pos + 1);
    }
    return result;
  }

  protected String getPath(String path) {
    String result = "";
    int pos = path.lastIndexOf('/');
    if (pos >= 0) {
      result = path.substring(0, pos);
    }
    return result;
  }

  protected List<String> getPathElements(String uriString) {

    List<String> list = new ArrayList<>();
    URI uri = URI.create(uriString);
    String parentPath = uri.getPath();

    String pathName = getPathName(parentPath);
    while (pathName.length() > 0) {
      list.add(0, pathName);
      parentPath = getPath(parentPath);
      pathName = getPathName((parentPath));
    }
    return list;
  }

  private void getAsync(AsyncContext asyncCtx) {
    LoggerService logger = new LoggerService(AioInfoServlet.class);
    AioDbManager db = new AioDbManager();

    HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    HttpServletResponse res = (HttpServletResponse) asyncCtx.getResponse();
    ServletContext sc = req.getServletContext();
    BasicDataSource ds = (BasicDataSource) sc.getAttribute("datasource");

    List<String> list = getPathElements(req.getRequestURI());
    logger.info(list.toString());
    if (list.size() > 3) {
      try {
        int pollTime = Integer.parseInt(list.get(3));
        if (pollTime < 0) {
          pollTime = 20;
        }
        res.setContentType("application/json");
        res.setCharacterEncoding("utf-8");
        PrintWriter out = res.getWriter();
        out.print(db.getDevicesInfoWithTime(ds, pollTime));
      } catch (NumberFormatException e) {
        logger.error("Invalid poll time. Couldn't fetch device information.");
      } catch (Exception e) {
        logger.error("Unable to retrieve Device Information.");
      }
    } else {
      try {
        res.setContentType("application/json");
        res.setCharacterEncoding("utf-8");
        PrintWriter out = res.getWriter();
        out.print(db.getDevicesInfo(ds));
      } catch (Exception e) {
        logger.error("Unable to retrieve Device Information.");
      }
    }
    asyncCtx.complete();
  }
}
