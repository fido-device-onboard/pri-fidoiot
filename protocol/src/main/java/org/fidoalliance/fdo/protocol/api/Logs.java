package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.fidoalliance.fdo.protocol.HttpUtils;

public class Logs extends RestApi{

  private File getLogFile() {
    return new File("service.log");
  }

  @Override
  public void doGet() throws Exception {

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);

    File file = getLogFile();
    try (BufferedReader br
        = new BufferedReader(new FileReader(file));) {

      String line;
      while ((line = br.readLine()) != null) {
        getResponse().getOutputStream().println(line);
      }
    }
  }

  @Override
  protected void doDelete() throws Exception {
    File file = getLogFile();
    Files.write(file.toPath(), new byte[0]);
  }
}
