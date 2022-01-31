package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.NullValueSerializer;

@JsonSerialize(using = NullValueSerializer.class)
public class NullValue {
}
