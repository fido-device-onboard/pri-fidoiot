// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.epid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EpidHttpClient {

  private static ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = Executors.defaultThreadFactory().newThread(r);
    t.setDaemon(true);
    return t;
  });
  private static final long httpRequestTimeout = Duration.ofSeconds(10).getSeconds();

  /**
   * Perform HTTP GET operation.
   *
   * @param url the Url to which request will be sent
   * @return the response from the Url
   * @throws IOException throws an IOException in case the response status code is not 200 (OK)
   */
  public static byte[] doGet(String url) throws IOException {
    HttpClient httpClient;
    try {
      httpClient = HttpClient.newBuilder().build();
      final HttpRequest.Builder httpRequestBuilder =
              HttpRequest.newBuilder().header("Accept", "application/octet-stream");
      final HttpRequest httpRequest = httpRequestBuilder.uri(URI.create(url)).GET().build();
      final Future<HttpResponse<byte[]>> future =
              executor.submit(() -> httpClient.send(httpRequest, BodyHandlers.ofByteArray()));
      final HttpResponse<byte[]> httpResponse;
      httpResponse = future.get(httpRequestTimeout, TimeUnit.SECONDS);
      if (httpResponse.statusCode() == 200) {
        return null != httpResponse.body() ? httpResponse.body() : new byte[0];
      } else {
        throw new IOException("HTTP GET failed with: " + httpResponse.statusCode());
      }
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform HTTP POST operation.
   *
   * @param url     the Url to which request will be sent
   * @param payload the data to send in HTTP request body
   * @return the status code response from the Url
   * @throws IOException throws an IOException in case the response status code is not 200 (OK)
   */
  public static int doPost(String url, String payload) throws IOException {
    HttpClient httpClient;
    try {
      httpClient = HttpClient.newBuilder().build();
      final HttpRequest.Builder httpRequestBuilder =
              HttpRequest.newBuilder().header("Content-Type", "application/json");
      final HttpRequest httpRequest =
          httpRequestBuilder.uri(URI.create(url)).POST(BodyPublishers.ofString(payload)).build();
      final Future<HttpResponse<String>> future =
          executor.submit(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
      final HttpResponse<String> httpResponse;
      httpResponse = future.get(httpRequestTimeout, TimeUnit.SECONDS);
      return httpResponse.statusCode();

    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
