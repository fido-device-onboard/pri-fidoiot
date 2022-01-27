// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.LinkedList;
import org.fidoalliance.fdo.protocol.serialization.GenericListSerializer;

@JsonSerialize(using = GenericListSerializer.class)
public class To2AddressEntries extends LinkedList<To2AddressEntry> {

}
