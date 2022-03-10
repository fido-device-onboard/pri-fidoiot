// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.lang3.function.FailableFunction;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

public interface VoucherQueryFunction
    extends FailableFunction<String, OwnershipVoucher, IOException> {
}
