// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.loggingutils.LoggerService;

public class AioFileUploadServlet extends HttpServlet {

  public static final LoggerService logger = new LoggerService(AioFileUploadServlet.class);

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
    if (req.getContentType().compareToIgnoreCase("application/octet-stream") != 0) {
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    final AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(0);
    new Thread(() -> putAsync(asyncCtx)).start();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
    try {
      final int pos = req.getRequestURI().lastIndexOf('/');
      final String fileName = req.getRequestURI().substring(pos + 1);
      if (fileName.startsWith(".") || fileName.indexOf('\\') > 0) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }
      final String path = getInitParameter(AioAppSettings.DOWNLOADS_PATH);

      try {
        Files.delete(Path.of(path, fileName));
      } catch (FileNotFoundException e) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } catch (IOException e) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      resp.setStatus(HttpServletResponse.SC_OK);
    } catch (Exception e) {
      logger.error("Unable to the delete files from AIO server.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void putAsync(AsyncContext asyncCtx) {

    final HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    final HttpServletResponse resp = (HttpServletResponse) asyncCtx.getResponse();

    try {
      final int pos = req.getRequestURI().lastIndexOf('/');
      final String fileName = req.getRequestURI().substring(pos + 1);
      if (fileName.startsWith(".") || fileName.indexOf('\\') > 0) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        asyncCtx.complete();
        return;
      }
      final String path = getInitParameter(AioAppSettings.DOWNLOADS_PATH);

      final String filePath = Path.of(path, fileName).toString();

      try (InputStream inStream = req.getInputStream();
           OutputStream outStream =
                   new BufferedOutputStream(new FileOutputStream(filePath))) {

        int byteRead = 0;
        while ((byteRead = inStream.read()) != -1) {
          outStream.write(byteRead);
        }
        outStream.flush();
      } catch (FileNotFoundException e) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } catch (IOException e) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      resp.setStatus(HttpServletResponse.SC_OK);
      asyncCtx.complete();
    } catch (Exception e) {
      logger.error("Error while adding file to AIO server.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
