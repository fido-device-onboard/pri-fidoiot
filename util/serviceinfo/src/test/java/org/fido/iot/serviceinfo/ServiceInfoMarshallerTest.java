// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ServiceInfoMarshallerTest {

  @Test
  void Test() throws Exception {
    ServiceInfoMarshaller marshaller = new ServiceInfoMarshaller(45, null);
    marshaller.register(new ServiceInfoModule() {

      @Override
      public void putServiceInfo(UUID uuid, ServiceInfoEntry entry) {

      }

      @Override
      public List<ServiceInfoEntry> getServiceInfo(UUID uuid) {
        List<ServiceInfoEntry> entries = new LinkedList<ServiceInfoEntry>();

        ServiceInfoEntry entry1 =
            new ServiceInfoEntry("devmod:device", new ServiceInfoSequence("DeviceId") {
              @Override
              public long length() {
                return "MNBVCXZASDFGHJKLPOIUYTREWQ123".length();
              }

              @Override
              public Object getContent() {
                long start = getStart();
                long end = getEnd();
                byte[] result = new byte[(int) (end - start)];
                java.nio.ByteBuffer.wrap("MNBVCXZASDFGHJKLPOIUYTREWQ123".getBytes())
                    .position((int) start).get(result, 0, (int) (end - start));
                return result;
              }

              @Override
              public boolean canSplit() {
                return true;
              }
            });
        entries.add(entry1);

        ServiceInfoEntry entry2 =
            new ServiceInfoEntry("devmod:active", new ServiceInfoSequence("IsActive") {
              @Override
              public long length() {
                return 1;
              }

              @Override
              public Object getContent() {
                return "F5";
              }

              @Override
              public boolean canSplit() {
                return false;
              }
            });
        entries.add(entry2);
        return entries;
      }
    });

    Iterable<Supplier<ServiceInfo>> serviceInfoIterable = marshaller.marshal();
    int mtuCounter = 0;
    for (final Iterator<Supplier<ServiceInfo>> it = serviceInfoIterable.iterator(); it.hasNext();) {
      ServiceInfo serviceInfo = it.next().get();
      if (mtuCounter == 0) {
        // 1st pass, only one partial entry fits as per MTU
        Assertions.assertEquals(1, serviceInfo.size());
        Assertions.assertEquals("devmod:device", serviceInfo.get(0).getKey());
      } else if (mtuCounter == 1) {
        // 2nd pass, 1 partial entry and 1 full entry fits as per MTU
        Assertions.assertEquals(2, serviceInfo.size());
        Assertions.assertEquals("devmod:device", serviceInfo.get(0).getKey());
        Assertions.assertEquals("devmod:active", serviceInfo.get(1).getKey());
      }
      mtuCounter++;
      Iterator<ServiceInfoEntry> marshalledEntries = serviceInfo.iterator();
      while (marshalledEntries.hasNext()) {
        ServiceInfoEntry marshalledEntry = marshalledEntries.next();
        String key = marshalledEntry.getKey();
        Object value = marshalledEntry.getValue().getContent();
        key.toCharArray();
      }
    }
  }

}
