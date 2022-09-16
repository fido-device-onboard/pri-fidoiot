package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableConsumer;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;

public interface ExceptionConsumer extends FailableConsumer<Throwable,IOException> {

}
