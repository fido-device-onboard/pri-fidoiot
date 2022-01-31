package org.fidoalliance.fdo.protocol;


import java.util.function.Function;

/**
 * Factory for log provider instances.
 */
public interface LogProviderFactory extends Function<Class<?>,LogProvider> {

}
