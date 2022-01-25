package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CoseKey;

public class CoseKeySerializer extends StdSerializer<CoseKey> {

  private static final int ELEMENT_COUNT = 3;

  private static final int COSEKEY_CRV = -1; //from RFC8152 13.1
  private static final int COSEKEY_X = -2; //from RFC8152 13.1
  private static final int COSEKEY_Y = -3; //from RFC8152 13.1

  public CoseKeySerializer() {
    this(null);
  }

  public CoseKeySerializer(Class<CoseKey> t) {
    super(t);
  }

  @Override
  public void serialize(CoseKey value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(ELEMENT_COUNT);
    } else {
      gen.writeStartObject(value,ELEMENT_COUNT);
    }

    //write the curve
    gen.writeFieldId(COSEKEY_CRV);
    gen.writeNumber(value.getCrv().toInteger());

    gen.writeFieldId(COSEKEY_X);
    gen.writeBinary(value.getX());

    gen.writeFieldId(COSEKEY_Y);
    gen.writeBinary(value.getY());

    gen.writeEndObject();
  }
}
