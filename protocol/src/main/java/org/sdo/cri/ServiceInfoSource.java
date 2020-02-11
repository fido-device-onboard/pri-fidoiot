// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * Supplies an ordered sequence of ServiceInfo key/value pairs for transmission
 * to the remote end of the protocol.
 *
 * <pre>
 *
 * The SDO ServiceInfo ecosystem includes:
 *
 * {@link PreServiceInfoMultiSource}
 * {@link PreServiceInfoSink}
 * {@link ServiceInfoSource}
 * {@link ServiceInfoSink}
 * {@link ServiceInfoMultiSource}
 * {@link ServiceInfoMultiSink}
 *
 * ----------------------------------------------------------------------------------------------
 * 'Source' classes are ServiceInfo suppliers.
 *
 * Sources implement{@literal Supplier<List<Entry<CharSequence, CharSequence>>>}:
 *
 *   The {@link Supplier} interface allows for lazy run-time evaluation.
 *
 *   A {@link List} is supplied because any supplier may return any number of
 *   ServiceInfo values.
 *
 *   ServiceInfo values are a key/value pair, implemented as a {@link Entry}.
 *
 *   ServiceInfo keys and values are {@link CharSequence}s:
 *
 *     The SDO protocol requires that ServiceInfo keys and data be character sequences (strings).
 *
 *     The SDO protocol requires that ServiceInfo data length be known before transmission.
 *     {@link CharSequence#length} provides this information.
 *
 *     Custom implementations of CharSequence are recommended when it is not practical for all
 *     of a ServiceInfo value to be in RAM at once.
 *
 * ----------------------------------------------------------------------------------------------
 * 'MultiSource' classes are ServiceInfo suppliers which use the device UUID as a key.
 * This allows them to customize their output for each device.
 *
 * MultiSources implement{@literal Function<UUID, List<Entry<CharSequence, CharSequence>>>}:
 *
 *   Supplier is not powerful enough for MultiSources.  Instead, the
 *   {@link java.util.function.Function} interface allows a {@link java.util.UUID} key
 *   to be supplied.
 *
 *   ServiceInfo values are returned in a {@link List} of identical construction
 *   to that returned by ServiceInfoSource.
 *
 * ----------------------------------------------------------------------------------------------
 * 'Sink' classes are ServiceInfo consumers.
 *
 * Sinks implement{@literal Consumer<Entry<CharSequence, CharSequence>>}:
 *
 *   The {@link java.util.function.Consumer} interface is used to accept ServiceInfo data.
 *
 *   The ServiceInfoData is implemented as a {@link Entry} of identical
 *   construction to the one used by the Source classes.
 *
 *   The {@link java.util.function.Consumer#accept} method of a Sink may be called many times.
 *   Data is not buffered in the protocol layer.  As soon as a message is complete, the protocol
 *   layer delivers it to all registered sinks and then forgets it.  Sinks must be prepared
 *   to be called many times, possibly with the same key repeated when value data is too big
 *   for a single protocol message.
 *
 * ----------------------------------------------------------------------------------------------
 * 'MultiSink' classes are ServiceInfo consumers which use the device UUID as a key.
 * This allows them to customize their response for each device.
 *
 * MultiSinks implement{@literal BiConsumer<UUID, Entry<CharSequence, CharSequence>>}:
 *
 *   The {@link java.util.function.BiConsumer} interface accepts device UUID and ServiceInfo data.
 *
 *   The ServiceInfo data is implemented as a {@link Entry} of identical
 *   construction to the one used by the Source classes.
 *
 * ----------------------------------------------------------------------------------------------
 * To create a new ServiceInfo module:
 *
 *   1. Create new device-side and owner-side scopes for the module.  Scopes may be designed
 *      in whatever way suits the situation best: a class, a package, an OSGI runtime, whatever.
 *      The only thing that matters to the SDO framework is the presence of Source and Sink
 *      interfaces.
 *
 *   2. If the new module will use pre-service information (PSI):
 *        - add an implementation of {@link PreServiceInfoMultiSource} to the owner side.
 *        - add an implementation of {@link PreServiceInfoSink} to the device side.
 *
 *   3. If the device side will transmit ServiceInfo:
 *        - add an implementation of {@link ServiceInfoSource} to the device side.
 *        - add an implementation of {@link ServiceInfoMultiSink} to the owner side.
 *
 *   4. If the owner side will transmit ServiceInfo:
 *        - add an implementation of {@link ServiceInfoMultiSource} to the owner side.
 *        - add an implementation of {@link ServiceInfoSink} to the device side.
 *
 * ----------------------------------------------------------------------------------------------
 * To install a ServiceInfo module when using the v2 Device and Owner objects:
 *
 *   1. Add its device-side {@link PreServiceInfoSink}s to the Device constructor arguments.
 *   2. Add its device-side {@link ServiceInfoSource}s to the {@link ServiceInfoMarshaller}
 *      which is given to the Device constructor.
 *   3. Add its device-side {@link ServiceInfoSink}s to the Device constructor arguments.
 *   4. Add its owner-side {@link PreServiceInfoMultiSource}s to the Owner constructor arguments.
 *   5. Add its owner-side {@link ServiceInfoMultiSource}s to the {@link ServiceInfoMarshaller}
 *      which is given to the Owner constructor.
 *   6. Add its owner-side {@link ServiceInfoMultiSink}s to the Owner constructor arguments.
 *
 *   All of this is intended to be handled via JSR-330 dependency injection, which makes it simpler.
 *
 *   Using Google Juice as your JSR-330 provider, an owner-side installation could look like:
 *
 *      ...
 *      Module bindings = new AbstractModule() {
 *        &#64;Override
 *        protected void configure() {
 *          ...
 *          Multibinder.newSetBinder(binder(), PreServiceInfoMultiSource.class)
 *            .addBinding().to(YourPreServiceInfoMultiSource.class);
 *          Multibinder.newSetBinder(binder(), ServiceInfoMultiSink.class);
 *            .addBinding().to(YourServiceInfoMultiSink.class);
 *          Multibinder.newSetBinder(binder(), ServiceInfoMultiSource.class);
 *            .addBinding().to(YourServiceInfoMultiSource.class);
 *          ...
 *      };
 *      ...
 *      Owner owner = Guice.createInjector(bindings).getInstance(Owner.class);
 * </pre>
 */
@FunctionalInterface
public interface ServiceInfoSource extends ServiceInfoModule {

  List<Entry<CharSequence, CharSequence>> getServiceInfo();
}
