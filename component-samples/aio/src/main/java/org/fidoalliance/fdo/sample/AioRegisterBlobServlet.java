package org.fidoalliance.fdo.sample;

import java.util.function.Consumer;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fidoalliance.fdo.loggingutils.LoggerService;

public class AioRegisterBlobServlet extends HttpServlet {

  LoggerService logger = new LoggerService(AioRegisterBlobServlet.class);
  public static final String BLOB_INJECTOR = "To0BlobInjector";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

    final int pos = req.getRequestURI().lastIndexOf('/');
    final String guid = req.getRequestURI().substring(pos + 1);

    if (guid.startsWith(".") || guid.indexOf('\\') > 0) {
      logger.warn("Received invalid guid: " + guid);
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      Consumer<String> injector =
          (Consumer<String>) req.getServletContext().getAttribute(BLOB_INJECTOR);
      injector.accept(guid);
      logger.info("Registered guid: " + guid);
    } catch (Exception ex) {
      logger.warn("Failed to register guid: " + guid);
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
