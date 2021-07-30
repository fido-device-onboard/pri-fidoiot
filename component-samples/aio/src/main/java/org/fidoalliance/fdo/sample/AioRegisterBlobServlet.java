package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.util.function.Consumer;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AioRegisterBlobServlet extends HttpServlet {

  public static final String BLOB_INJECTOR = "To0BlobInjector";

  private void getAsync(AsyncContext asyncCtx) {

    final HttpServletRequest req = (HttpServletRequest) asyncCtx.getRequest();
    final HttpServletResponse resp = (HttpServletResponse) asyncCtx.getResponse();

    final int pos = req.getRequestURI().lastIndexOf('/');
    final String guid = req.getRequestURI().substring(pos + 1);
    if (guid.startsWith(".") || guid.indexOf('\\') > 0) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      asyncCtx.complete();
      return;
    }

    try {
      Consumer<String> injector = (Consumer<String>) req.getServletContext()
          .getAttribute(BLOB_INJECTOR);
      injector.accept(guid);
    } finally {
      asyncCtx.complete();
    }

  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    final AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(0);
    new Thread(() -> getAsync(asyncCtx)).start();

  }
}
