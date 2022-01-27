package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableFunction;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.To1dPayload;

public interface RvBlobQueryFunction  extends
    FailableFunction<String, CoseSign1, IOException> {

}
