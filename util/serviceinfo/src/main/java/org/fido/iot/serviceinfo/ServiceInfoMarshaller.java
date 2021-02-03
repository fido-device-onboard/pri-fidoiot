// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ServiceInfoMarshaller implements Serializable {

  private long mtu;
  private UUID uuid;
  private int index = 0; // index for fetching entry from entries
  private long subSequenceStart = 0; // index until which the value has been read completely

  private transient List<ServiceInfoModule> serviceInfoModules;
  private transient List<ServiceInfoEntry> serviceInfoEntries;

  // some constants that help to determine Integer CBOR type.
  private static final double POWER_8 = Math.pow(2, 8) - 1;
  private static final double POWER_16 = Math.pow(2, 16) - 1;

  public ServiceInfoMarshaller(long mtu, UUID uuid) {
    this.mtu = mtu;
    this.uuid = uuid;
  }

  private void init() {
    serviceInfoModules = new LinkedList<ServiceInfoModule>();
    serviceInfoEntries = new LinkedList<ServiceInfoEntry>();
  }

  /**
   * Resets the positions.
   */
  public void reset() {
    index = 0;
    subSequenceStart = 0;
  }

  /**
   * Register a service info module.
   * 
   * @param serviceInfoModule instance of {@link ServiceInfoModule}
   */
  public void register(ServiceInfoModule serviceInfoModule) {
    init();
    serviceInfoModules.add(serviceInfoModule);
    serviceInfoEntries.addAll(serviceInfoModule.getServiceInfo(this.uuid));
  }

  /**
   * Breaks service info into chunks of MTU.
   *
   * @return MTU packet
   */
  public Iterable<Supplier<ServiceInfo>> marshal() {
    return new LazyIterable(serviceInfoEntries, mtu);
  }

  private class LazyIterable implements Iterable<Supplier<ServiceInfo>> {

    private final List<ServiceInfoEntry> entries;
    private final long mtu;

    LazyIterable(final List<ServiceInfoEntry> entries, final long mtu) {
      this.entries = entries;
      this.mtu = mtu;
    }

    List<ServiceInfoEntry> getEntries() {
      return this.entries;
    }

    long getMtu() {
      return this.mtu;
    }

    @Override
    public Iterator<Supplier<ServiceInfo>> iterator() {
      return new LazyIterator(this);
    }
  }

  private class LazyIterator implements Iterator<Supplier<ServiceInfo>> {

    private final LazyIterable iterable;

    LazyIterator(final LazyIterable iterable) {
      this.iterable = iterable;
    }

    @Override
    public boolean hasNext() {
      return index < iterable.getEntries().size();
    }

    @Override
    public Supplier<ServiceInfo> next() {
      return pack();
    }

    // Return the expected CBOR length (in bytes) of the input number.
    private int additionalCborLength(long length) {
      if (length <= 23) {
        return 1;
      } else if (length > 23 && length <= POWER_8 - 1) {
        return 2;
      } else if (length > (POWER_8 - 1) && length < (POWER_16 - 1)) {
        return 3;
      }
      // not supporting anything more than 2^16 as MTU
      return 0;
    }

    // service info is off the form [[[modName:messageName, value],[modName:messageName, value]]]
    private Supplier<ServiceInfo> pack() {
      // List to store the innermost arrays of form [modName:messageName, value]
      final List<ServiceInfoEntry> subList = new LinkedList<>();
      final long mtu = iterable.getMtu();
      final List<ServiceInfoEntry> entries = iterable.getEntries();

      // [[[]]], reserving 1 byte each to accommodate outer array and mid array.
      long packed = 2;
      // track midArray size
      int midArraySize = additionalCborLength(subList.size());
      while (packed < mtu && index < entries.size()) {
        if (midArraySize < additionalCborLength(subList.size())) {
          packed += 1;
          midArraySize = additionalCborLength(subList.size());
        }

        ServiceInfoEntry entry = entries.get(index);
        int keyLength = entry.getKey().length();
        long valueLength = entry.getValue().length();
        boolean toSplit = entry.getValue().canSplit();

        // check if the value allows itself to be split.
        if (!toSplit) {
          if (keyLength + additionalCborLength(keyLength) + valueLength
              + additionalCborLength(valueLength) > mtu) {
            // size of entry is more than the MTU, skip entry
            index++;
            break;
          } else if (packed + keyLength + additionalCborLength(keyLength) + valueLength
              + additionalCborLength(valueLength) > mtu) {
            // size of entry + the total packed length yet, is more than MTU
            // pack in the next iteration
            break;
          } else if (packed + keyLength + additionalCborLength(keyLength) + valueLength
              + additionalCborLength(valueLength) < mtu) {
            packed += 1; // 1 byte for creating a new array (innermost block of [[[]]])
            subList.add(new ServiceInfoEntry(entry.getKey(), entry.getValue()));
            index++;
            continue;
          } else {
            throw new RuntimeException();
          }
        }

        // Account for the key only if the key can be placed fully inside the array, with
        // atleast 2 bytes left for value, to avoid sending both partial/full key with no value
        if (packed + keyLength + additionalCborLength(keyLength) < mtu - 2) {
          packed += 1; // 1 byte for creating a new array (innermost block of [[[]]])
          packed += keyLength + additionalCborLength(keyLength);

          // find how much of the CBOR encoded value fits
          long valueFitLen = 0;

          // calculate total length of the remaining value
          long valueRemaining = valueLength - subSequenceStart;
          if (valueRemaining > 0) {
            long encodeLen = 0; // look-ahead counter
            for (;;) {
              // look-ahead if the incremented value is going to fit within MTU
              encodeLen = valueFitLen + 1;
              if ((packed + encodeLen + additionalCborLength(encodeLen)) > mtu) {
                break; // if encoded length does not fit then done packing
              }
              valueFitLen++;
              if (valueFitLen == valueRemaining) {
                break; // if the we are at the end of sequence then done packing
              }
            }
            packed += valueFitLen + additionalCborLength(valueFitLen);
          }

          if (valueFitLen <= 0) {
            break; // nothing fits so break for next mtu
          }

          // now we know end of sequence
          long subSequenceEnd = subSequenceStart + valueFitLen;

          if (valueFitLen == valueRemaining) {
            // everything fits so move to next entry
            if (subSequenceStart == 0) {
              subList.add(entry); // no update to start and end positions needed
            } else {
              // update the start and end positions
              entry.getValue().updateSequence(subSequenceStart, subSequenceEnd);
              subList.add(new ServiceInfoEntry(entry.getKey(), entry.getValue()));
            }
            subSequenceStart = 0;
            index++;
          } else {
            // partial fit
            entry.getValue().updateSequence(subSequenceStart, subSequenceEnd);
            subList.add(new ServiceInfoEntry(entry.getKey(), entry.getValue()));
            subSequenceStart = subSequenceEnd;
          }
        } else {
          break; // can't add any more entries
        }
      }
      ServiceInfo serviceInfo = new ServiceInfo();
      serviceInfo.addAll(subList);
      return () -> serviceInfo;
    }
  }
}
