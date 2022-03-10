// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableConsumer;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;

public interface DeviceCredentialConsumer extends FailableConsumer<DeviceCredential,IOException> {
}
