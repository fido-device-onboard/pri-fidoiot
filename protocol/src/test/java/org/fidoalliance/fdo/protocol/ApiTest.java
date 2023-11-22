package org.fidoalliance.fdo.protocol;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApiTest {

  SessionFactory sessionFactory;
  StandardHttpServer standardHttpServer;

  @BeforeAll
  public void stub() {
    String path = "src/test/resources";
    String resourcePath = new File(path).getAbsolutePath();
    System.setProperty("fdo.config.home", resourcePath);

    sessionFactory = HibernateUtil.getSessionFactory();
    standardHttpServer = new StandardHttpServer();
    standardHttpServer.run();
  }

  public void doGetApi(String url, String value, int resp) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (value != "") {
      assert (response.body().toString().equals(value));
    }
    assert(resp == response.statusCode());
  }

  public int doGetApi(String url, String value) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (value != "") {
      assert (response.body().toString().equals(value));
    }
    return response.statusCode();
  }

  public int doGetFileApi(String url, String fileName) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Files.writeString(Path.of(fileName), response.body().toString());
    return response.statusCode();
  }

  public void  doGetFileApi(String url, String fileName, int resp) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Files.writeString(Path.of(Config.getPath(), fileName), response.body().toString());
    assert (resp == response.statusCode());
  }

  public int doPostApi(String url,String body) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofMinutes(1))
          .header("Content-Type", "text/plain")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.statusCode();
  }

  public void doPostApi(String url, String body, int resp) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(1))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assert (resp == response.statusCode());
  }

  public void doPostFileApi(String url, String filename, int resp) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    Path path = Path.of(filename);
    if (!Path.of(filename).toFile().exists()) {
       path = Path.of(Config.getPath(), filename);
    }
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(1))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofFile(path))
            .build();
    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assert (resp == response.statusCode());

  }

  public int doDeleteApi(String url,String value) throws IOException, InterruptedException {
    URL urlEndpoint = new URL(url);
    HttpURLConnection httpCon = (HttpURLConnection) urlEndpoint.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestProperty("Content-Type", "text/plain" );
    httpCon.setRequestMethod("DELETE");

    try (OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream(), StandardCharsets.UTF_8)) {
      out.write(value);
    }

    httpCon.connect();
    return httpCon.getResponseCode();
  }

  public void doDeleteApi(String url, String value, int resp) throws IOException, InterruptedException {
    URL urlEndpoint = new URL(url);
    HttpURLConnection httpCon = (HttpURLConnection) urlEndpoint.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestProperty("Content-Type", "text/plain" );
    httpCon.setRequestMethod("DELETE");

    try (OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream(), StandardCharsets.UTF_8)) {
      out.write(value);
    }

    httpCon.connect();
    assert (resp == httpCon.getResponseCode());
  }

  @Test
  public void healthTest() throws IOException, InterruptedException {
      doGetApi("http://localhost:8080/health", "", HttpServletResponse.SC_OK);
  }

  @Test
  public void certificateValidityTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/certificate/validity?days=abcd", "",
            HttpServletResponse.SC_BAD_REQUEST);

    Config.getWorker(ValidityDaysSupplier.class).get();
    doGetApi("http://localhost:8080/api/v1/certificate/validity", "10800",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void deviceInfoTest() throws IOException, InterruptedException {
    doGetApi("http://localhost:8080/api/v1/deviceinfo", "[]",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/deviceinfo/100", "[]",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/deviceinfo/-10", "[]",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/deviceinfo/1000000", "[]",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void aioRvInfoTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/aio/rvinfo?ip=127.0.0.1&rvprot=https", "",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/aio/rvinfo?rvprot=https", "",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/aio/rvinfo?ip=127.0.0.1", "",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/aio/rvinfo?ip=127.0.0.1&rvprot=http", "",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void allowListTest() throws IOException, InterruptedException {
    String pemCertificate = "-----BEGIN CERTIFICATE-----\n"
            + "MIIBIjCByaADAgECAgkApNMDrpgPU/EwCgYIKoZIzj0EAwIwDTELMAkGA1UEAwwC\n"
            + "Q0EwIBcNMTkwNDI0MTQ0NjQ3WhgPMjA1NDA0MTUxNDQ2NDdaMA0xCzAJBgNVBAMM\n"
            + "AkNBMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELAJwkDKz/BaWq1Wx7PjkR5W5\n"
            + "LLIbamgSZeVNUlyFM/t0sMAxAWbvEbDzKu924TX4as3WVjMmfekysx30PlDGJaMQ\n"
            + "MA4wDAYDVR0TBAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiEApUGbgjYT0k63AeRA\n"
            + "tPM2i+VnW6ckYaJyvFLuuWw+QUACIE5w0ntjHLbvwmqgwCfh5T6u8exQdCA2g9Hs\n"
            + "u53hKcaS\n"
            + "-----END CERTIFICATE-----\n";

    doPostApi("http://localhost:8080/api/v1/rv/allow", pemCertificate,
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/rv/allow", pemCertificate,
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/rv/allow", pemCertificate,
            HttpServletResponse.SC_BAD_REQUEST);
    doPostApi("http://localhost:8080/api/v1/rv/allow", pemCertificate,
            HttpServletResponse.SC_OK);
  }

  @Test
  public void logTest() throws IOException, InterruptedException {
    doDeleteApi("http://localhost:8080/api/v1/logs", "",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void sviSizeTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/owner/svisize", "abcd",
            HttpServletResponse.SC_BAD_REQUEST);
    doPostApi("http://localhost:8080/api/v1/owner/svisize", "300",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/owner/svisize", "10000",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/owner/svisize", "76000",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/owner/svisize", "65536",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void rvInfoTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/rvinfo",
            "[[[2,\"127.0.0.1\"],[5,\"127.0.0.1\"],[3,8040],[4,8040]]]",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/rvinfo",
            "[[[2,\"127.0.0.1\"],[5,\"127.0.0.1\"],[3,8080],[4,8080]]]",
            HttpServletResponse.SC_OK);
     doGetApi("http://localhost:8080/api/v1/rvinfo",
            "[[[2,\"127.0.0.1\"],[5,\"127.0.0.1\"],[3,8080],[4,8080]]]",
             HttpServletResponse.SC_OK);
  }

  @Test
  public void certificateTest() throws IOException, InterruptedException {
    doGetApi("http://localhost:8080/api/v1/certificate?alias=axb12-dsad-das", "",
            HttpServletResponse.SC_NOT_FOUND);
    doGetFileApi("http://localhost:8080/api/v1/certificate?alias=SECP256R1", "owner_pub_key",
            HttpServletResponse.SC_OK);
    doPostFileApi("http://localhost:8080/api/v1/certificate?filename=ssl.p12", "ssl.p12",
            HttpServletResponse.SC_OK);
    doPostFileApi("http://localhost:8080/api/v1/certificate?filename=mfs.p12", "ssl.p12",
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/certificate?filename=mfs.p12", "",
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/certificate?filename=invalid.p12", "",
            HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void redirectEntryTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/owner/redirect",
            "[[\"127.0.0.1\",\"127.0.0.1\",8042,3]]",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/owner/redirect",
            "[[\"127.0.0.1\",\"127.0.0.1\",8042,3]]",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/owner/redirect",
            "[[\"localhost\",\"127.0.0.1\",8080,3]]",
            HttpServletResponse.SC_OK);
  }

  @Test
  public void systemResourceTest() throws IOException, InterruptedException {
    doPostApi("http://localhost:8080/api/v1/owner/svi",
            "[{\"filedesc\" : \"setup.sh\"}]",
            HttpServletResponse.SC_OK);
    doGetApi("http://localhost:8080/api/v1/owner/svi",
            "[{\"filedesc\" : \"setup.sh\"}]",
            HttpServletResponse.SC_OK);
    doPostApi("http://localhost:8080/api/v1/owner/svi",
            "[{\"fildesc\" : \"setup.sh\"}]",
            HttpServletResponse.SC_BAD_REQUEST);
    doPostApi("http://localhost:8080/api/v1/owner/svi", "",
            HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void systemResourceFileTest() throws IOException, InterruptedException {
    doPostFileApi("http://localhost:8080/api/v1/owner/resource?filename=ssl.p12", "ssl.p12",
            HttpServletResponse.SC_OK);
    doGetFileApi("http://localhost:8080/api/v1/owner/resource?filename=ssl.p12", "ssl.p12",
            HttpServletResponse.SC_OK);
    doPostFileApi("http://localhost:8080/api/v1/owner/resource?filename=mfg.p12", "ssl.p12",
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/owner/resource?filename=mfg.p12", "",
            HttpServletResponse.SC_OK);
    doDeleteApi("http://localhost:8080/api/v1/owner/resource?filename=invalid.p12", "",
            HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void zipDiSampleTest() throws IOException, InterruptedException {

    DiSample diApplication = new DiSample();
    diApplication.run();
    doGetApi("http://localhost:8080/api/v1/to0/" + DiSample.getGuid(), "",
            HttpServletResponse.SC_OK);
  }

  @AfterAll
  public void tearDown() {
      HibernateUtil.shutdown();
  }
}



