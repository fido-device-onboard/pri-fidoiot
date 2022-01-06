package org.fidoalliance.fdo.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.StreamMessage;


public abstract class HttpClient implements Runnable {

  private DispatchMessage request;
  private DispatchMessage response;
  private String serverUri;
  private String tokenCache;

  protected DispatchMessage getRequest() {
    return request;
  }

  protected DispatchMessage getResponse() {
    return response;
  }

  protected static LoggerService logger = new LoggerService(HttpClient.class);

  protected String getServerUri() {
    return serverUri;
  }

  protected void setServerUri(String serverUri) {
    this.serverUri = serverUri;
  }

  protected void setRequest(DispatchMessage request) {
    this.request = request;
  }

  protected void setResponse(DispatchMessage response) {
    this.response = response;
  }


  public abstract void generateHello() throws IOException;

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

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      URIBuilder uriBuilder = new URIBuilder(getServerUri());
      List<String> segments = new ArrayList<>();
      segments.add(HttpUtils.FDO_COMPONENT);
      segments.add(ProtocolVersion.current().toString());
      segments.add(HttpUtils.MSG_COMPONENT);
      segments.add(Integer.toString(getRequest().getMsgType().toInteger()));
      uriBuilder.setPathSegments(segments);

      HttpPost httpRequest = new HttpPost(uriBuilder.build());

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

    } catch (ClientProtocolException e) {
      throw new IOException(e);
    } catch (IOException e) {
      throw new IOException(e);
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

  }

  @Override
  public void run() {

    setRequest(null);
    setResponse(null);
    try {
      StandardMessageDispatcher dispatcher = Config.getWorker(StandardMessageDispatcher.class);

      generateHello();
      for (; ; ) {

        logMessage(getRequest());

        sendMessage();

        logMessage(getResponse());

        Optional<DispatchMessage> nextMsg = dispatcher.dispatch(getResponse());
        if (nextMsg.isPresent()) {
          DispatchMessage msg = nextMsg.get();

          if (msg.getMsgType() == MsgType.ERROR) {
            break;
          }
          setRequest(msg);
        } else {
          break;
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
