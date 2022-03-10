// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.util.Date;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableSupplier;

public interface AcceptOwnerFunction extends FailableBiFunction<String, Long, Date, IOException> {

}
