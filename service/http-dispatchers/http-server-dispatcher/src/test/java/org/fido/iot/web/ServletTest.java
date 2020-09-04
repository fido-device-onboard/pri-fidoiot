// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Path;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;

public class ServletTest {

  private static final int RV_PORT = 8040;
  private static final String HOST_NAME = "localhost";
  private static final String PROTOCOL_NAME = "http://";
  private static final String WEB_PATH = "/fido/100/msg";

  private static final String BASE_PATH = "target";

  private String getMessagePath(int msgId) {
    return WEB_PATH + "/" + Integer.toString(msgId);
  }

  private String getUrl(int msgId) {
    return PROTOCOL_NAME + HOST_NAME + ":" + Integer.toString(RV_PORT) + getMessagePath(msgId);
  }

  @Test
  void Test() throws Exception {
    Tomcat tomcat = new Tomcat();

    tomcat.setPort(RV_PORT);

    //set the path of tomcat
    Path basePath = Path.of(System.getProperty("user.dir"), BASE_PATH, "tomcat");
    System.setProperty("catalina.home", basePath.toString());

    Context ctx = tomcat.addContext("", null);
    ctx.addApplicationListener(TestListener.class.getName());

    Wrapper wrapper = tomcat.addServlet(ctx, "rvServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO0_HELLO));
    wrapper.addMapping(getMessagePath(Const.TO0_OWNER_SIGN));
    wrapper.setAsyncSupported(true);

    tomcat.getConnector();
    tomcat.start();

    String url = getUrl(Const.TO0_HELLO);
    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
        .uri(URI.create(url));

    byte[] body = Composite.newArray().toBytes();
    reqBuilder.setHeader("Content-Type", Const.HTTP_APPLICATION_CBOR);
    reqBuilder
        .setHeader(Const.HTTP_AUTHORIZATION,
            Const.HTTP_BEARER + " " + TestListener.BEARER_TOKEN);
    reqBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body));

    HttpClient hc = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .sslContext(SSLContext.getInstance("TLS"))
        .sslParameters(new SSLParameters())
        .build();

    HttpResponse<byte[]> hr = hc.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

    assertTrue(hr.statusCode() == Const.HTTP_OK);
    Composite reply = Composite.fromObject(hr.body());
    byte[] nonce3 = reply.getAsBytes(Const.FIRST_KEY);

    //get bearer token from http
    for (String value : hr.headers().allValues(Const.HTTP_AUTHORIZATION)) {
      int start = 0;
      int end = value.indexOf(' ');
      if (end > start) {
        String authType = value.substring(start, end);
        if (authType.compareToIgnoreCase(Const.HTTP_BEARER) == 0) {
          String token = value.substring(end + 1);
          assertTrue(token.equals(TestListener.BEARER_TOKEN));
        }
      }
    }

    tomcat.stop();
  }
}
