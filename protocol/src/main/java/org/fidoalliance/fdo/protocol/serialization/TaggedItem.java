// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import org.fidoalliance.fdo.protocol.message.CborTags;

public interface TaggedItem {

  CborTags getTag();
}
