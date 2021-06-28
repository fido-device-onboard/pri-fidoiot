package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows download of files using curl or wget.
 */
public class AioFileDownloadServlet extends HttpServlet {

  // common http setting
  public static final int HTTP_OK = 200;
  public static final int HTTP_NOT_FOUND = 404;
  public static final int HTTP_SERVER_ERROR = 500;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

    final AsyncContext asyncCtx = req.startAsync();

    asyncCtx.setTimeout(0);

    new Thread(() -> getAsync(asyncCtx)).start();
  }

  private void getAsync(AsyncContext asyncCtx) {

    final HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    final HttpServletResponse res = (HttpServletResponse) asyncCtx.getResponse();

    res.setContentType("application/octet-stream");

    final int pos = req.getRequestURI().lastIndexOf('/');
    final String fileName = req.getRequestURI().substring(pos + 1);
    final String path = getInitParameter(AioAppSettings.DOWNLOADS_PATH);

    String filePath = Path.of(path, fileName).toString();
    try (FileInputStream inStream = new FileInputStream(filePath);
        OutputStream outStream = res.getOutputStream()) {
      int byteRead = 0;
      while ((byteRead = inStream.read()) != -1) {
        outStream.write(byteRead);
      }
      outStream.flush();
    } catch (FileNotFoundException e) {
      res.setStatus(HTTP_NOT_FOUND);
    } catch (IOException e) {
      res.setStatus(HTTP_SERVER_ERROR);
    }
    res.setStatus(HTTP_OK);
    asyncCtx.complete();
  }
}
