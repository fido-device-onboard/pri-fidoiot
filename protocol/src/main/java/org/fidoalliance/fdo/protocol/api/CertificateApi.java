package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemFormatter;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;


/***
 *  CertificateAPI REST endpoint enables users to
 *     - Collect the keystores stored in the DB based on the filename.
 *     - Upload new keystores to database.
 *     - Delete existing keystores in database.
 *
 *  Accepted URL patterns :
 *     - GET /api/v1/certificate?filename=<filename>
 *     - POST  /api/v1/certificate?filename=<filename> with filecontents in the body
 *     - DELETE /api/v1/certificate?filename=<filename>
 *
 *  RestApi Class provides a wrapper over the HttpServletRequest methods.
 *
 */

public class CertificateApi extends RestApi {

  LoggerService logger = new LoggerService(CertificateApi.class);

  @Override
  public void doGet() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest
    String fileName = getParamByValue("filename");
    String alias = getParamByValue("alias");
    String uuid = getParamByValue("uuid");

    if (uuid != null) {

      OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, uuid);

      if (onboardingVoucher != null) {
        OwnershipVoucher voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getData(),
            OwnershipVoucher.class);
        String keyAlias = VoucherUtils.getPublicKeyAlias(voucher);

        getResponse().getWriter().print("[\"alias\":\"" + keyAlias + "\"]");
      }
      return;
    }

    if (alias != null) {

      KeyResolver resolver = Config.getWorker(OwnerKeySupplier.class).get();
      Certificate[] chain = resolver.getCertificateChain(alias);
      if (chain == null) {
        throw new NotFoundException(alias);
      }
      String pem = PemFormatter.format(Arrays.asList(chain));
      getResponse().getWriter().print(pem);
      return;
    }

    // Query database table CERTIFICATE_DATA for filename Key
    CertificateData certificateData = getSession().get(CertificateData.class, fileName);

    if (certificateData != null) {
      byte[] data = certificateData.getData();
      getResponse().getOutputStream().write(data);
    } else {
      logger.warn("Keystore file not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }

  @Override
  public void doPost() throws IOException {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest parameter and
    // uploaded file content from InputStream of request.
    String fileName = getParamByValue("filename");
    byte[] keyStoreFile = getRequest().getInputStream().readAllBytes();

    // Query database table CERTIFICATE_DATA for filename Key
    CertificateData certificateData = getSession().get(CertificateData.class, fileName);

    if (certificateData == null) {
      // Insert the row, if filename doesn't exist.
      certificateData = new CertificateData();
      certificateData.setName(fileName);
      certificateData.setData(keyStoreFile);
      getSession().save(certificateData);
    } else {
      // Update the row, if filename already exists.
      certificateData.setData(keyStoreFile);
      getSession().update(certificateData);
    }

  }

  @Override
  public void doDelete() {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'filename' from HttpRequest
    String fileName = getParamByValue("filename");

    // Query database table CERTIFICATE_DATA for filename Key
    CertificateData certificateData = getSession().get(CertificateData.class, fileName);

    if (certificateData != null) {
      // delete the row, if data exists.
      getSession().delete(certificateData);
    } else {
      logger.warn("Keystore file not found.");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
