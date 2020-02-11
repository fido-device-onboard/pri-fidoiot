// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.owner;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.sdo.cri.MessageType;
import org.sdo.cri.ProtocolMessage;
import org.sdo.cri.ProtocolService;
import org.sdo.cri.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.async.DeferredResult;

@Controller
public class OwnerWebController {

  private static final String BEARER = "Bearer ";

  private final ExecutorService myExecutorService;
  private final Set<ProtocolServiceBuilder> myProtocolServiceBuilders;
  private final ProtocolServiceStorage myProtocolServiceStorage;

  /**
   * Construct a new object.
   *
   * @param executorService         The ExecutorService for asynchronous tasks
   * @param protocolServiceBuilders The ServiceBuilders providing service objects
   * @param protocolServiceStorage  The storage for our protocol services
   */
  public OwnerWebController(
      ExecutorService executorService,
      Set<ProtocolServiceBuilder> protocolServiceBuilders,
      ProtocolServiceStorage protocolServiceStorage) {

    this.myExecutorService = Objects.requireNonNull(executorService);
    this.myProtocolServiceBuilders = Objects.requireNonNull(protocolServiceBuilders);
    this.myProtocolServiceStorage = Objects.requireNonNull(protocolServiceStorage);
  }

  private Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }

  @PostMapping("mp/{versionId}/msg/{messageId}")
  DeferredResult<ResponseEntity<?>> onPost(
      @PathVariable int versionId,
      @PathVariable int messageId,
      RequestEntity<String> requestEntity) {

    logger().debug(requestEntity.toString());

    final String requestBody = requestEntity.hasBody() ? requestEntity.getBody() : "";
    DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>();

    // If we can't convert version or message type to a known enum, that's a 404.
    final Version version;
    final MessageType messageType;
    try {
      version = Version.valueOfInt(versionId);
      messageType = MessageType.valueOfInt(messageId);
    } catch (Exception e) {
      deferredResult.setResult(ResponseEntity.notFound().build());
      return deferredResult;
    }

    final ProtocolMessage sdoRequest = new ProtocolMessage() {
      @Override
      public String getBody() {
        return requestBody;
      }

      @Override
      public Version getVersion() {
        return version;
      }

      @Override
      public MessageType getType() {
        return messageType;
      }
    };

    myExecutorService.submit(() -> {
      try {
        ProtocolService protocolService = null;
        // Are we continuing an existing session?
        final String auth = requestEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (null != auth && auth.startsWith(BEARER)) {
          try {
            final UUID sessionId = UUID.fromString(auth.substring(BEARER.length()));
            protocolService = myProtocolServiceStorage.take(sessionId);
          } catch (IllegalArgumentException e) {
            // the bearer token isn't a UUID and we'll treat it like a lookup failure
            protocolService = null;
          }

          if (null == protocolService) {
            // The session was invalid or not in our lookup table, so fail the request
            deferredResult.setResult(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            return;
          }
        } else { // no auth, might be a hello
          Iterator<ProtocolServiceBuilder> builderIt = myProtocolServiceBuilders.iterator();
          while (null == protocolService && builderIt.hasNext()) {
            final ProtocolService candidate = builderIt.next().build();
            if (candidate.isHello(sdoRequest)) {
              protocolService = candidate;
            }
          }
        }

        if (null == protocolService) {
          // If we still haven't found a service, we're out of luck
          deferredResult.setResult(ResponseEntity.notFound().build());
          return;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        final ProtocolMessage sdoResponse = protocolService.next(sdoRequest);

        if (!(null == sdoResponse
            || MessageType.ERROR == sdoResponse.getType()
            || protocolService.isDone())) {

          final UUID sessionId = myProtocolServiceStorage.put(protocolService);
          responseBuilder = responseBuilder
              .header(HttpHeaders.AUTHORIZATION, BEARER + sessionId.toString());
        }

        final ResponseEntity<String> responseEntity =
            responseBuilder.body(null != sdoResponse ? sdoResponse.getBody() : "");
        logger().debug(responseEntity.toString());

        deferredResult.setResult(responseEntity);

      } catch (Throwable t) {
        deferredResult.setErrorResult(t);
      }
    });

    return deferredResult;
  }
}
