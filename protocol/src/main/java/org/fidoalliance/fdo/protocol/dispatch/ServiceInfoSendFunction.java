package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;

public interface ServiceInfoSendFunction extends
    FailableFunction<ServiceInfoKeyValuePair, Boolean, IOException> {
}
