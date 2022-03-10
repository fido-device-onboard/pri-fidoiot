// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import org.apache.commons.lang3.function.FailableFunction;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;

public interface CertSignatureFunction
    extends FailableFunction<ManufacturingInfo, Certificate[], IOException> {
}
