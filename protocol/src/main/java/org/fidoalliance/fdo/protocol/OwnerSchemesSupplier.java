// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.function.FailableSupplier;

public interface OwnerSchemesSupplier extends FailableSupplier<List<String>, IOException> {

}
