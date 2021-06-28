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

public class AioFileUploadServlet extends HttpServlet {

  // common http setting
  public static final int HTTP_OK = 200;
  public static final int HTTP_NOT_FOUND = 404;
  public static final int HTTP_SERVER_ERROR = 500;
  public static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;


  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
    if (req.getContentType().compareToIgnoreCase("application/octet-stream") != 0) {
      resp.setStatus(HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    final AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(0);
    new Thread(() -> putAsync(asyncCtx)).start();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
    final int pos = req.getRequestURI().lastIndexOf('/');
    final String fileName = req.getRequestURI().substring(pos + 1);
    final String path = getInitParameter(AioAppSettings.DOWNLOADS_PATH);

    try {
      Files.delete(Path.of(path, fileName));
    } catch (FileNotFoundException e) {
      resp.setStatus(HTTP_NOT_FOUND);
    } catch (IOException e) {
      resp.setStatus(HTTP_SERVER_ERROR);
    }
    resp.setStatus(HTTP_OK);
  }

  private void putAsync(AsyncContext asyncCtx) {

    final HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    final HttpServletResponse resp = (HttpServletResponse) asyncCtx.getResponse();

    final int pos = req.getRequestURI().lastIndexOf('/');
    final String fileName = req.getRequestURI().substring(pos + 1);
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
      resp.setStatus(HTTP_NOT_FOUND);
    } catch (IOException e) {
      resp.setStatus(HTTP_SERVER_ERROR);
    }
    resp.setStatus(HTTP_OK);
    asyncCtx.complete();
  }

}
