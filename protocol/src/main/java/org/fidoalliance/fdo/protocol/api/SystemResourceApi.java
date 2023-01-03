// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.SystemResource;

/**
 * SystemResourceApi REST endpoint enables users to - Collect the svi resource files stored in the
 * DB(based on the filename). - Upload new svi resource files to SYSTEM_RESOURCE table. - Delete
 * existing svi resource files in SYSTEM_RESOURCE table.
 *
 * <p>Accepted URL patterns :
 *   - GET /api/v1/sviresource?filename=&lt;filename&gt;
 *   - POST /api/v1/sviresource?filename=&lt;filename&gt;  with filecontents in the body
 *   - DELETE /api/v1/sviresource?filename=&lt;filename&gt;
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */

public class SystemResourceApi extends RestApi {

  private static final LoggerService logger = new LoggerService(SystemResourceApi.class);

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
      getResponse().getOutputStream().write(data.getBytes(1, (int) data.length()));
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
    logger.info("Uploaded File: " + fileName + "to SystemResource Table");
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
