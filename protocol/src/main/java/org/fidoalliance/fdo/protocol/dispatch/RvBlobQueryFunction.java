// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableFunction;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.To1dPayload;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;

public interface RvBlobQueryFunction  extends
    FailableFunction<String, To2RedirectEntry, IOException> {

}
