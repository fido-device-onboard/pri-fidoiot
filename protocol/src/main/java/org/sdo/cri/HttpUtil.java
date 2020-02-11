// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

class HttpUtil {

  static int OK_200 = 200;

  static String AUTHORIZATION = "Authorization";
  static String CONTENT_TYPE = "Content-Type";

  static String APPLICATION_JSON = "application/json";

  static String dump(HttpRequest request) {
    StringBuilder sb = new StringBuilder();
    request.bodyPublisher().ifPresent(bp -> {
      bp.subscribe(new Subscriber<>() {
        @Override
        public void onSubscribe(Subscription subscription) {
          subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer bb) {
          sb.append(StandardCharsets.US_ASCII.decode(bb));
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
      });
    });

    return "HttpRequest: "
        + request.toString()
        + '\n'
        + request.headers().map().toString()
        + '\n'
        + sb.toString();
  }

  static String dump(HttpResponse response) {

    return "HttpResponse: "
        + response.toString()
        + '\n'
        + response.headers().map().toString()
        + '\n'
        + response.body().toString();
  }
}
