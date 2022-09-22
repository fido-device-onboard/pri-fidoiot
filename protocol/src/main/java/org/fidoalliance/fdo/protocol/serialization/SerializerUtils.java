// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.lang.reflect.Field;

public class SerializerUtils {

  /**
   * Gets an annotated property value.
   * @param object The object to get annotated value.
   * @param name The name of the property.
   * @return The value of the property.
   * @throws IOException An error occurred.
   */
  public static Object getPropertyValue(Object object, String name) throws IOException {

    Class theClass = object.getClass();
    Object result = null;

    for (; ; ) {
      Field[] fields = theClass.getDeclaredFields();

      for (Field field : fields) {
        JsonProperty[] properties =
            field.getDeclaredAnnotationsByType(JsonProperty.class);
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


  /**
   * Get the list of annotated property names.
   * @param value The object to get the annotated property names.
   * @return An array of annotated property names.
   */
  public static String[] getPropertyNames(Object value) {
    Class theClass = value.getClass();

    String[] propertyOrder = null;

    for (; ; ) {
      JsonPropertyOrder[] orders =
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
