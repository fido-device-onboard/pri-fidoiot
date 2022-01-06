package org.fidoalliance.fdo.protocol;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.apache.commons.lang3.math.NumberUtils;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;

public class HttpUtils {

  public static final String HTTP_AUTHORIZATION = "Authorization";
  public static final String HTTP_APPLICATION_CBOR = "application/cbor";
  public static final String HTTP_PLAIN_TEXT = "text/plain";
  public static final String HTTP_MESSAGE_TYPE = "Message-Type";
  public static final String HTTP_CONTENT_TYPE = "Content-Type";


  public static final String FDO_COMPONENT = "fdo";
  public static final String MSG_COMPONENT = "msg";

  private static final int  URI_PART_FDO = 0;
  private static final int  URI_PART_PROTOCOL = 1;
  private static final int  URI_PART_MSG = 2;
  private static final int  URI_PART_MSG_ID = 3;

  public static DispatchMessage getMessageFromURI(String uri) throws IOException {

    //URI is in form /fdo/<protocolver>/msg/<msgType>
    DispatchMessage message = new DispatchMessage();


    File segFile = new File(uri);
    if (NumberUtils.isCreatable(segFile.getName())) {
      message.setMsgType(MsgType.fromNumber(Integer.parseInt(segFile.getName())));
    } else {
      throw new InvalidPathException(uri,"msgType not a number");
    }
    segFile = segFile.getParentFile();
    if (!segFile.getName().equals(HttpUtils.MSG_COMPONENT)) {
      throw new InvalidPathException(uri,"msg expected");
    }

    segFile = segFile.getParentFile();
    if (NumberUtils.isCreatable(segFile.getName())) {
      message.setProtocolVersion(ProtocolVersion.fromString(segFile.getName()));
    } else {
      throw new InvalidPathException(uri,"protocol version not a number");
    }

    segFile = segFile.getParentFile();
    if (!segFile.getName().equals(HttpUtils.FDO_COMPONENT)) {
      throw new InvalidPathException(uri,"fdo expected");
    }

    return message;
  }
}
