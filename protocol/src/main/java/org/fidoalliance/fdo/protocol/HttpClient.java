// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;


public abstract class HttpClient implements Runnable {

  private DispatchMessage request;
  private DispatchMessage response;
  private List<HttpInstruction> httpInst;
  private String tokenCache;


  protected static final LoggerService logger = new LoggerService(HttpClient.class);

  protected DispatchMessage getRequest() {
    return request;
  }

  protected DispatchMessage getResponse() {
    return response;
  }

  protected List<HttpInstruction> getInstructions() {
    return httpInst;
  }

  protected int index = 0;

  protected void finishedOk() {

  }

  protected void setInstructions(List<HttpInstruction> httpInst) {
    this.httpInst = httpInst;
  }

  protected void setRequest(DispatchMessage request) {
    this.request = request;
  }

  protected void setResponse(DispatchMessage response) {
    this.response = response;
  }

  protected abstract void generateHello() throws IOException;

  protected void generateBypass() throws IOException {

  }

  protected void clearByPass() throws IOException {

  }

  protected void initializeSession() throws IOException {
    setRequest(new DispatchMessage());
    getRequest().setExtra(new SimpleStorage());
  }

  protected void logMessage(DispatchMessage msg) {
    StringBuilder builder = new StringBuilder();
    builder.append("Type ");
    builder.append(msg.getMsgType().toInteger());
    builder.append(" ");
    try {
      Mapper.INSTANCE.writeDiagnostic(builder,
          Mapper.INSTANCE.readValue(msg.getMessage(), AnyType.class));
    } catch (Exception e) {
      builder.append("failed to covert to diagnostic form.");
    }

    logger.info(builder.toString());
  }

  protected void sendMessage() throws IOException {
    URI requestUri = null;
    if (getRequest().getMsgType() == MsgType.TO1_HELLO_RV
        || getRequest().getMsgType() == MsgType.TO2_HELLO_DEVICE
        || getRequest().getMsgType() == MsgType.TO0_HELLO) {
      index = 0;
    }
    HttpInstruction httpInstruction = null;
    long delay = 0L;
    while (index <= getInstructions().size()) {

      try (CloseableHttpClient httpClient = Config.getWorker(HttpClientSupplier.class).get()) {

        MsgType msgId = getRequest().getMsgType();
        httpInstruction = getInstructions().get(index);
        URIBuilder uriBuilder = new URIBuilder(
            httpInstruction.getAddress());
        delay = getInstructions().get(index).getDelay();
        if (msgId == MsgType.TO1_HELLO_RV) {
          if (httpInstruction.isRendezvousBypass()) {
            generateBypass();
            //this will change bypass
            msgId = MsgType.TO2_HELLO_DEVICE;
          } else {
            logger.info("RVBypass flag not set, Starting TO1.");
            logger.info("TO1 URL is " + uriBuilder);
          }

        }
        if (msgId == MsgType.TO2_HELLO_DEVICE) {
          if (httpInstruction.isRendezvousBypass()) {
            logger.info("RVBypass flag is set, Skipped T01.");
          }
          logger.info("TO2 URL is " + uriBuilder);
        } else if (msgId == MsgType.TO0_HELLO) {
          logger.info("TO0 URL is " + uriBuilder);
        }

        List<String> segments = new ArrayList<>();

        segments.add(HttpUtils.FDO_COMPONENT);
        segments.add(ProtocolVersion.current().toString());
        segments.add(HttpUtils.MSG_COMPONENT);
        segments.add(Integer.toString(msgId.toInteger()));
        uriBuilder.setPathSegments(segments);

        requestUri = uriBuilder.build();

        HttpPost httpRequest = new HttpPost(requestUri);

        ByteArrayEntity bae = new ByteArrayEntity(getRequest().getMessage());
        httpRequest.setEntity(bae);

        httpRequest.addHeader(HttpUtils.HTTP_CONTENT_TYPE, HttpUtils.HTTP_APPLICATION_CBOR);
        if (tokenCache != null) {
          httpRequest.addHeader(HttpUtils.HTTP_AUTHORIZATION, tokenCache);
        }

        logMessage(getRequest());

        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
          HttpEntity entity = httpResponse.getEntity();
          if (entity != null) {
            if (entity.getContentLength() == 0 && getRequest().getMsgType() == MsgType.ERROR) {
              break;
            }
            setResponse(new DispatchMessage());
            getResponse().setProtocolVersion(getRequest().getProtocolVersion());

            if (entity.getContentLength() > BufferUtils.getMaxBufferSize()) {
              throw new MessageBodyException("Message too large.");
            }
            getResponse().setMessage(EntityUtils.toByteArray(entity));

            if (httpResponse.containsHeader(HttpUtils.HTTP_MESSAGE_TYPE)) {
              getResponse().setMsgType(MsgType.fromNumber(
                  Integer.valueOf(
                      httpResponse.getFirstHeader(HttpUtils.HTTP_MESSAGE_TYPE).getValue())));
            }

            if (httpResponse.containsHeader(HttpUtils.HTTP_AUTHORIZATION)) {
              tokenCache = httpResponse.getFirstHeader(HttpUtils.HTTP_AUTHORIZATION).getValue();
            }
            getResponse().setAuthToken(tokenCache);
            logMessage(getResponse());
          } else {
            continue;
          }
        }
        break; // success

      } catch (RuntimeException e) {
        if (getInstructions().size() > 0
            && index < getInstructions().size()
            && (getRequest().getMsgType() == MsgType.TO1_HELLO_RV
            || getRequest().getMsgType() == MsgType.TO2_HELLO_DEVICE
            || getRequest().getMsgType() == MsgType.TO0_HELLO)) {

          if (httpInstruction.isRendezvousBypass()) {
            clearByPass();
          }
          logger.info("instruction failed.");
          logger.info("moving to next instruction");
          index++;
          if (delay > 0) {
            logger.info("Delaying connection for next " + delay + " seconds");
            try {
              Thread.sleep(1000 * delay);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(ex);
            }
          }
          continue;
        }

        logger.info("all instructions exhausted");
        throw new IOException(e);

      } catch (Exception e) {

        try {
          Config.getWorker(ExceptionConsumer.class).accept(e);
        } catch (Exception innerException) {
          logger.error("failed log exception");
        }

        if (getInstructions().size() > 0
            && index < getInstructions().size()
            && (getRequest().getMsgType() == MsgType.TO1_HELLO_RV
            || getRequest().getMsgType() == MsgType.TO2_HELLO_DEVICE
            || getRequest().getMsgType() == MsgType.TO0_HELLO)) {

          if (httpInstruction.isRendezvousBypass()) {
            clearByPass();
          }
          logger.info("instruction failed.");
          logger.info("moving to next instruction");
          index++;
          continue;
        }

        logger.info("all instructions exhausted");

        if (getRequest().getMsgType() == MsgType.TO0_HELLO) {
          logger.info("Failed TO0 with error: " + e.getMessage());
        }
        
        throw new IOException(e);
      }

    }

  }

  @Override
  public void run() {

    setRequest(null);
    setResponse(null);
    try {
      StandardMessageDispatcher dispatcher = Config.getWorker(StandardMessageDispatcher.class);

      initializeSession();
      generateHello();
      while (getInstructions().size() > 0) {

        sendMessage();

        getResponse().setExtra(getRequest().getExtra());

        Optional<DispatchMessage> nextMsg = dispatcher.dispatch(getResponse());
        if (nextMsg.isPresent()) {
          DispatchMessage msg = nextMsg.get();
          msg.setExtra(response.getExtra());
          setRequest(msg);

        } else {
          finishedOk();
          if (getResponse().getMsgType() == MsgType.TO1_RV_REDIRECT) {
            generateHello();
          } else {
            break;

          }
        }

      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to establish connection with FDO Server");
    } catch (Throwable throwable) {
      if (getResponse() != null
          && getResponse().getMsgType() != MsgType.ERROR) {
        DispatchMessage errorMsg = DispatchMessage.fromThrowable(throwable, getRequest());
        setRequest(errorMsg);

        try {
          sendMessage();
        } catch (Throwable e) {
          logger.error("failed to send error");
        }
      }

    }

  }

}
