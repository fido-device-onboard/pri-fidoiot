package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousMedium;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;
import org.hibernate.mapping.Any;

public class RendezvousInstructionDeserializer extends StdDeserializer<RendezvousInstruction> {

  public RendezvousInstructionDeserializer() {
    this(null);
  }

  public RendezvousInstructionDeserializer(Class<?> t) {
    super(t);
  }

  private AnyType getSubValue(RendezvousVariable variable,JsonNode subNode)
      throws UnknownHostException {
    switch (variable) {
      case WIFI_SSID:
      case WIFI_PW:
      case DNS:

        return AnyType.fromObject(subNode.textValue());
      case IP_ADDRESS:

         return AnyType.fromObject(
              InetAddress.getByName(subNode.textValue()).getAddress());
      case OWNER_PORT:
      case DEV_PORT:
      case PROTOCOL:
      case MEDIUM:
      case DELAYSEC:
        return AnyType.fromObject(subNode.intValue());
      case CL_CERT_HASH:
      case SV_CERT_HASH:
      case EXT_RV:
        return AnyType.fromObject(subNode);
      case USER_INPUT:
        return AnyType.fromObject(subNode.booleanValue());
     default:
        return null;
    }
  }

  @Override
  public RendezvousInstruction deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);


    RendezvousInstruction rvi = new RendezvousInstruction();

    int rvv = node.get(0).intValue();
    RendezvousVariable variable = RendezvousVariable.fromNumber(rvv);
    rvi.setVariable(variable);

    if (node.size() > 1) {
      JsonNode subNode = node.get(1);
      if (subNode.isBinary()) {
        rvi.setValue(AnyType.fromObject(subNode.binaryValue()));
      } else {
        AnyType any = getSubValue(variable,subNode);
        if (any == null) {
          throw new JsonParseException(jp,"invalid rv variable " + variable.toString());
        }
        any.wrap();
        rvi.setValue(any);
      }
    }
    //now check




    return rvi;
  }
}

  /*Device Only	None	RVDevOnly	none	Both
  Owner Only	None	RVOwnerOnly	none	Both
  IP address	None	RVIPAddress	IPAddress	Both
  DNS name	None	RVDns	DNSAddress	Both
Port, Device	Based on protocol	RVDevPort	UInt16	Device
    Port, Owner	Based on protocol	RVOwnerPort	uint16	Owner
    TLS Server cert hash	None	RVSvCertHash	Hash	Device
    TLS CA cert hash	None	RVClCertHash	Hash	Device
    User input	No	RVUserInput	bool	Device
    SSID	None	RVWifiSsid	tstr	Device
    Wireless Password	None	RVWifiPw	tstr	Device
    Medium	Device dependent	RVMedium	$RVMediumValue(uint8)	Device
    Protocol	TLS	RVProtocol	RVProtocolValue(uint8)	Device
    Delay	0	RVDelaysec	uint32	Both
    Bypass	none	RVBypass	none	Device
    External RV	none	RVExtRV	array*/