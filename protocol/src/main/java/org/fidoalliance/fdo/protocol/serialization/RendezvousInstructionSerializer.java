package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;

public class RendezvousInstructionSerializer extends StdSerializer<RendezvousInstruction> {


  public RendezvousInstructionSerializer() {
    this(null);
  }

  public RendezvousInstructionSerializer(Class<RendezvousInstruction> t) {
    super(t);
  }

  @Override
  public void serialize(RendezvousInstruction value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    RendezvousVariable rvv = value.getVariable();
    int count = 2;
    if (rvv.equals(RendezvousVariable.DEV_ONLY)
        || rvv.equals(RendezvousVariable.OWNER_ONLY )
        || rvv.equals(RendezvousVariable.BYPASS)) {
      count = 1;
    }

    gen.writeStartArray(value, count);

    gen.writeNumber(rvv.toInteger());
    if (count > 1) {
      gen.writeObject(value.getValue());
    }


    gen.writeEndArray();
  }

}
