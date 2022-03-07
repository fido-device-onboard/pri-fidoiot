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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;


public abstract class HttpClient implements Runnable {

  private DispatchMessage request;
  private DispatchMessage response;
  private List<HttpInstruction> httpInst;
  private String tokenCache;


  protected static LoggerService logger = new LoggerService(HttpClient.class);

  protected DispatchMessage getRequest() {
    return request;
  }

  protected DispatchMessage getResponse() {
    return response;
  }

  protected List<HttpInstruction> getInstructions() {
    return httpInst;
  }

  protected boolean isRepeatable(String uri) {
    if (uri.endsWith("/30")) {
      return true;
    }
    return false;
  }

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
    int index = 0;
    URI requestUri = null;
    int delay = 0;
    for (; ; ) {

      try (CloseableHttpClient httpClient = Config.getWorker(HttpClientSupplier.class).get()) {

        MsgType msgId = getRequest().getMsgType();
        HttpInstruction httpInstruction = getInstructions().get(index++);
        URIBuilder uriBuilder = new URIBuilder(
            httpInstruction.getAddress());

        if (msgId == MsgType.TO1_HELLO_RV) {
          if (!httpInstruction.isRendezvousBypass()) {
            logger.info("RVBypass flag not set, Starting TO1.");
          }
          logger.info("TO1 URL is " + uriBuilder.toString());
        } else if (msgId == MsgType.TO2_HELLO_DEVICE) {
          if (!httpInstruction.isRendezvousBypass()) {
            logger.info("RVBypass flag is set, Skipped T01.");
          }
          logger.info("TO2 URL is " + uriBuilder.toString());
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

        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest);) {
          HttpEntity entity = httpResponse.getEntity();
          if (entity != null) {
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
          } else {
            throw new IOException(httpResponse.getStatusLine().toString());
          }
        }
        break; // success

      } catch (Exception e) {
        if (getInstructions().size() > 1
            && index < getInstructions().size()) {
          continue;
        }
        /*if (isRepeatable(requestUri.getPath())) {
          try {
            Thread.sleep(Duration.ofSeconds(delay).toMillis());
          } catch (InterruptedException ex) {
            throw new IOException(ex);
          }
          index =0;
        }*/
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
      for (; ; ) {

        logMessage(getRequest());

        sendMessage();

        logMessage(getResponse());
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
    } catch (Throwable throwable) {
      if (getResponse() != null) {
        DispatchMessage errorMsg = DispatchMessage.fromThrowable(throwable, getRequest());
        setRequest(errorMsg);
        logMessage(errorMsg);
        try {
          sendMessage();
        } catch (Throwable e) {
          logger.error("failed to send error");
        }
      }

    }

  }

}
