package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.PemFormatter;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import org.fidoalliance.fdo.protocol.entity.SystemResource;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.security.cert.Certificate;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;


/***
 *  SystemResourceApi REST endpoint enables users to
 *     - Collect the svi resource files stored in the DB(based on the filename).
 *     - Upload new svi resource files to SYSTEM_RESOURCE table.
 *     - Delete existing svi resource files in SYSTEM_RESOURCE table.
 *
 *  Accepted URL patterns :
 *     - GET /api/v1/sviresource?filename=<filename>
 *     - POST  /api/v1/sviresource?filename=<filename> with filecontents in the body
 *     - DELETE /api/v1/sviresource?filename=<filename>
 *
 *  RestApi Class provides a wrapper over the HttpServletRequest methods.
 *
 */

public class SystemResourceApi extends RestApi {

  LoggerService logger = new LoggerService(SystemResourceApi.class);

  @Override
  public void doGet() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest
    String fileName = getParamByValue("filename");

    // Query database table SYSTEM_RESOURCE for filename Key
    SystemResource sviResource = getSession().get(SystemResource.class, fileName);

    if (sviResource != null) {
      Blob data = sviResource.getData();
      getResponse().getOutputStream().write(data.getBytes(1,(int)data.length()));
    } else {
      logger.warn("SVI resource file not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }

  @Override
  public void doPost() throws IOException, SQLException {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest parameter and
    // uploaded file content from InputStream of request.
    String fileName = getParamByValue("filename");
    byte[] sviResourceFile = getRequest().getInputStream().readAllBytes();

    // Query database table SYSTEM_RESOURCE for filename Key
    SystemResource sviResource = getSession().get(SystemResource.class, fileName);

    if (sviResource == null) {
      // Insert the row, if filename doesn't exist.
      sviResource = new SystemResource();
      sviResource.setName(fileName);
      sviResource.setData(new SerialBlob(sviResourceFile));
      getSession().save(sviResource);
    } else {
      // Update the row, if filename already exists.
      sviResource.setData(new SerialBlob(sviResourceFile));
      getSession().update(sviResource);
    }

  }

  @Override
  public void doDelete() {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest
    String fileName = getParamByValue("filename");

    // Query database table SYSTEM_RESOURCE for filename Key
    SystemResource sviResource = getSession().get(SystemResource.class, fileName);

    if (sviResource != null) {
      // delete the row, if data exists.
      getSession().delete(sviResource);
    } else {
      logger.warn("SVI resource file not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
