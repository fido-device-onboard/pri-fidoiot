package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.storage.OwnerDbManager;

public class OwnerDeviceTypeCriteriaServlet extends HttpServlet {

  private static final String DELIMITER = " ";
  private static final LoggerService logger =
      new LoggerService(OwnerDeviceTypeCriteriaServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      logger.warn("Request failed because of invalid content type.");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());
      if (requestBody != null) {

        String[] deviceTypeCriteria = requestBody.split(DELIMITER);
        if (deviceTypeCriteria.length != 3) {
          logger.warn("Request failed because of invalid input.");
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

      }
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String deviceType = req.getParameter("devicetype");
    if (deviceType == null) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      //DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      //new OwnerDbManager().removeDeviceTypeCriteria(ds, deviceType);
    } catch (Exception exp) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
