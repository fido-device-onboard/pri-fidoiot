package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;

public interface KeyStoreOutputStreamFunction
    extends FailableFunction<String, OutputStream, IOException> {
}
