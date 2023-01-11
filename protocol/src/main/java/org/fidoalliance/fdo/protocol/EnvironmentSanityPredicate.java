package org.fidoalliance.fdo.protocol;

import java.util.Map;
import org.apache.commons.lang3.function.FailableBiPredicate;

public interface EnvironmentSanityPredicate extends FailableBiPredicate<Map, String, Exception> {

}
