// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.io.Serializable;

/**
 * Class that contains information on the cursor start and end positions that a single ServiceInfo
 * entry will be read from. The implementation for the abstract methods must provide a mechanism to
 * fetch the service info length and serviceinfo content based on the cursor start and end
 * positions. The length() and canSplit() are supposed to be called upon exclusively by
 * {@link ServiceInfoMarshaller}, while the rest are to be called on per-use basis.
 */
public abstract class ServiceInfoSequence implements Serializable {

  private String serviceInfoId; // serviceinfo id
  private long start; // inclusive index
  private long end; // exclusive index

  /**
   * Constructor.
   * 
   * @param id service info identifier that is used to track service info
   */
  public ServiceInfoSequence(String id) {
    this.serviceInfoId = id;
  }

  public void initSequence() {
    this.start = 0;
    this.end = length();
  }

  protected void updateSequence(long start, long end) {
    this.start = start;
    this.end = end;
  }

  protected String getServiceInfoId() {
    return serviceInfoId;
  }

  /**
   * Return index (inclusive) from which content should be read.
   * 
   * @return
   */
  protected long getStart() {
    return start;
  }

  /**
   * Return index (exclusive) until which content should be read.
   * 
   * @return
   */
  protected long getEnd() {
    return end;
  }

  /**
   * Return the length (in bytes) of the content this Sequence represents, in case the sequence
   * represents an object to be packed by {@link ServiceInfoMarshaller}. Return '0' otherwise.
   * 
   * @return length as long value
   */
  public abstract long length();

  /**
   * Return the actual content of the Sequence, from 'start' (inclusive) and 'end' (exclusive), or
   * the entire content. The type and encoding of content returned (CBOR/String/Integer and others)
   * is dependent on the implementation. See methods getStart() and getEnd().
   * 
   * @return Object representing the content
   */
  public abstract Object getContent();

  /**
   * Return boolean true if the content as received from method 'getContent()' can be split
   * partially to allow transfer of the sequence across messages.
   * 
   * @return value
   */
  public abstract boolean canSplit();
}
