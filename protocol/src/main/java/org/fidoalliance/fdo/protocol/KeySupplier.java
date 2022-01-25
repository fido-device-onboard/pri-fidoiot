// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableSupplier;

public interface KeySupplier extends FailableSupplier<KeyResolver, IOException> {

}
