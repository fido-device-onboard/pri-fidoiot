package org.fido.iot.api;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerDeviceTypeCriteriaServlet extends HttpServlet {

  private static final String DELIMITER = " ";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());
      if (requestBody != null) {

        DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
        OwnerDbManager ownerDbManager = new OwnerDbManager();

        String[] deviceTypeCriteria = requestBody.split(DELIMITER);
        if (deviceTypeCriteria.length != 3) {
          getServletContext().log("Invalid request has been provided.");
          resp.setStatus(400);
          return;
        }

        ownerDbManager.addDeviceTypeCriteria(
            ds, deviceTypeCriteria[0], deviceTypeCriteria[1], deviceTypeCriteria[2]);
      }
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String deviceType = req.getParameter("devicetype");
    if (deviceType == null) {
      resp.setStatus(400);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      new OwnerDbManager().removeDeviceTypeCriteria(ds, deviceType);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
