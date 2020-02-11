// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A utility class for marshalling a sequence of key/value service info values into a sequence of
 * ServiceInfo messages.
 *
 * <p>This class should be treated as internal to this implementation.
 */
final class ServiceInfoMarshaller implements Serializable {

  private static final int BASE64_CHARS_PER_BLOCK = 4;
  private Long mtu = 1300L; // protocol specification suggests a default of 1300
  private List<ServiceInfoMultiSource> multiSources = new ArrayList<>();
  private List<ServiceInfoSource> sources = new ArrayList<>();

  /**
   * Construct a new object using default settings.
   */
  public ServiceInfoMarshaller() {
  }

  /**
   * Construct a new object.
   *
   * @param mtu The maximum desired length of service info messages.
   * @param multiSources The service info multisources to poll for data to marshal.
   * @param sources The service info sources to poll for data to marshal.
   */
  public ServiceInfoMarshaller(
      long mtu,
      List<ServiceInfoMultiSource> multiSources,
      List<ServiceInfoSource> sources) {

    this.mtu = mtu;
    this.multiSources = multiSources;
    this.sources = sources;
  }

  private Long getMtu() {
    return mtu;
  }

  public void setMtu(Long value) {
    mtu = value;
  }

  private List<ServiceInfoMultiSource> getMultiSources() {
    return multiSources;
  }

  public void setMultiSources(List<ServiceInfoMultiSource> value) {
    multiSources = value;
  }

  private List<ServiceInfoSource> getSources() {
    return sources;
  }

  public void setSources(List<ServiceInfoSource> value) {
    sources = value;
  }

  /**
   * Marshal for transmission all available ServiceInfo for the given UUIDs.
   *
   * <p>Poll all the registered sources to collect service info for the given UUIDs,
   * marshalling that data as an iterable sequence of ServiceInfo suppliers.
   *
   * <p>Suppliers are used because service info data can be quite large and expensive to fetch
   * and we don't want to pay the costs associated with that until we're actually ready to
   * transmit.
   *
   * <p>This is a simple implementation which builds a list of records which can,
   * in turn, be used to build the final ServiceInfos. This is easy, but the list of records could
   * itself be really big. An optimized lazy implementation of Iterable could save space, should the
   * need arise.
   *
   * @param uuids The UUIDs for which to marshal serviceInfo.
   * @return An Iterable of ServiceInfo Suppliers.
   */
  public Iterable<Supplier<ServiceInfo>> marshal(UUID... uuids) {

    // ServiceInfoSources provide key/value 'entries' in an ordered sequence with no awareness
    // of how those entries will be carried by the transport layer. Value data is concealed behind
    // CharSequence interfaces because we need both lazy reads and deterministic subsequences.
    //
    List<Entry<CharSequence, CharSequence>> allEntries =
        concatenateAllServiceInfoEntries(uuids);
    return new LazyIterable(allEntries, getMtu());
  }

  // Assemble all provided service info entries into a homogeneous list
  //
  private List<Entry<CharSequence, CharSequence>> concatenateAllServiceInfoEntries(UUID... uuids) {

    final List<Entry<CharSequence, CharSequence>> allEntries =
        new LinkedList<>();

    for (final ServiceInfoSource source : getSources()) {
      allEntries.addAll(source.getServiceInfo());
    }

    for (final ServiceInfoMultiSource multiSource : getMultiSources()) {
      for (UUID uuid : uuids) {
        allEntries.addAll(multiSource.getServiceInfo(uuid));
      }
    }

    return allEntries;
  }

  private class LazyIterable implements Iterable<Supplier<ServiceInfo>>, Serializable {

    private final List<Entry<CharSequence, CharSequence>> entries;
    private final long mtu;

    LazyIterable(
        final List<Entry<CharSequence, CharSequence>> entries,
        final long mtu) {

      this.entries = entries;
      this.mtu = mtu;
    }

    List<Entry<CharSequence, CharSequence>> getEntries() {
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

  private class LazyIterator implements Iterator<Supplier<ServiceInfo>>, Serializable {

    private int index;
    private final LazyIterable iterable;
    private int subSequenceStart;

    LazyIterator(final LazyIterable iterable) {
      this.iterable = iterable;
      this.index = 0;
      this.subSequenceStart = 0;
    }

    @Override
    public boolean hasNext() {
      return index < iterable.getEntries().size();
    }

    @Override
    public Supplier<ServiceInfo> next() {
      return pack();
    }

    private Supplier<ServiceInfo> pack() {

      final List<Entry<CharSequence, CharSequence>> subList =
          new ArrayList<>();

      final long mtu = iterable.getMtu();
      final List<Entry<CharSequence, CharSequence>> entries =
          iterable.getEntries();

      long packed = 2; // '{' '}'

      while (packed < mtu && index < entries.size()) {

        if (subList.size() > 0) {
          packed++; // ,
        }

        packed += 5; // " " : " "
        Entry<CharSequence, CharSequence> entry = entries.get(index);
        packed += entry.getKey().length();

        // find how much of the base64 encoded value fits
        int valueFitLen = 0;
        int valueRemaining = entry.getValue().length() - this.subSequenceStart;
        if (valueRemaining > 0) {
          int encodeLen = 0;
          for (; ; ) {
            // calculate base64 encoded length
            encodeLen = (((valueFitLen + 1) + 2) / 3) * 4;
            if ((packed + encodeLen) > mtu) {
              break;// if encoded length does not fit then done packing
            }
            valueFitLen++;
            if (valueFitLen == valueRemaining) {
              break; // if the we are at the end of sequence then done packing
            }
          }
          packed += encodeLen;
        }

        if (valueFitLen <= 0) {
          break; // nothing fits so break for next mtu
        }

        // now we know end of sequence
        int subSequenceEnd = subSequenceStart + valueFitLen;

        if (valueFitLen == valueRemaining) {
          // everything fits so move to next entry
          if (subSequenceStart == 0) {
            subList.add(entry); // no subSequence needed
          } else {
            // Subsequence needed
            subList.add(new SimpleEntry<>(entry.getKey(),
                entry.getValue().subSequence(subSequenceStart, subSequenceEnd)));
          }
          subSequenceStart = 0;
          index++;
        } else {
          // partial fit
          subList.add(new SimpleEntry<>(entry.getKey(),
              entry.getValue().subSequence(subSequenceStart, subSequenceEnd)));
          subSequenceStart = subSequenceEnd;
        }
      }

      ServiceInfo serviceInfo = new ServiceInfo();
      serviceInfo.addAll(subList);
      return () -> serviceInfo;
    }
  }
}
