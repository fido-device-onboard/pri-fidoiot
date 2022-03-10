// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

public interface VoucherStorageFunction  extends
    FailableBiFunction<String, OwnershipVoucher, UUID, IOException> {

}
