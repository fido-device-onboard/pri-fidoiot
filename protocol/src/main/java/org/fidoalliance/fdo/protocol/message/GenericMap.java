package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import org.fidoalliance.fdo.protocol.serialization.GenericMapSerializer;

@JsonFormat(shape = Shape.OBJECT)
@JsonSerialize(using = GenericMapSerializer.class)
public class GenericMap extends HashMap<Object,Object> {

}
