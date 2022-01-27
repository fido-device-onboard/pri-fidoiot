package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.AnyType;

public class SerializerUtils {

  public static Object getPropertyValue(Object object, String name) throws IOException {

    Class theClass = object.getClass();
    Object result = null;

    for (; ; ) {
      Field[] fields = theClass.getDeclaredFields();

      for (Field field : fields) {
        JsonProperty properties[] =
            (JsonProperty[]) field.getDeclaredAnnotationsByType(JsonProperty.class);
        for (JsonProperty property : properties) {
          if (property.value().equals(name)) {

            field.setAccessible(true);
            try {
              result = field.get(object);
              break;
            } catch (IllegalAccessException e) {
              throw new IOException(e);
            }
          }
          if (result != null) {
            break;
          }
        }
      }

      theClass = theClass.getSuperclass();

      if (theClass == null) {
        break;
      }
    }
    return result;
  }



  public static String[] getPropertyNames(Object value) {
    Class theClass = value.getClass();

    String[] propertyOrder = null;

    for (; ; ) {
      JsonPropertyOrder orders[] =
          (JsonPropertyOrder[]) theClass.getDeclaredAnnotationsByType(JsonPropertyOrder.class);

      for (JsonPropertyOrder order : orders) {
        propertyOrder = order.value();
        break;
      }

      theClass = theClass.getSuperclass();
      if (theClass == null) {
        break;
      }

    }

    if (propertyOrder == null) {
      propertyOrder = new String[0];
    }
    return propertyOrder;
  }





}
