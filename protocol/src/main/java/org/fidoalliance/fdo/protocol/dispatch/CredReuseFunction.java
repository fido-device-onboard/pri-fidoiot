// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.function.FailableFunction;

public interface CredReuseFunction extends FailableFunction<Boolean, Boolean, IOException> {

}
