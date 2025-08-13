/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.telemetry.api;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;
import io.helidon.telemetry.spi.TelemetryProvider;

/**
 * Contract for a telemetry implementation.
 * <p>
 * A telemetry implementation handles one or more <em>signals</em>&mdash;sources of telemetry&mdash;for example, tracing,
 * metrics, and logging.
 */
@Service.Contract
public interface Telemetry /* extends NamedService */ {

    /**
     * Config key for telemetry settings.
     */
    String CONFIG_KEY = "telemetry";

    /**
     * Returns a neutral telemetry builder.
     *
     * @return telemetry builder
     */
    static Builder builder() {
        return Services.get(TelemetryProvider.class).telemetryBuilder();
    }

    /**
     * Returns the signal of the requested type, if present.
     *
     * @param signalType type of signal (e.g, {@code Tracing}
     * @return the {@link Telemetry.Signal} of the type
     * @param <T> type of the signal
     */
    <T> Optional<Signal<T>> signal(Class<T> signalType);

    /**
     * Unwraps the telemetry signal implementation as the specified type.
     *
     * @param type to which to convert
     *
     * @return the unwrapped value
     * @param <B> type to which to convert
     */
    <B> B unwrap(Class<B> type);

    /**
     * Shuts down telemetry.
     */
    void close();

    /**
     * Abstraction of a telemetry signal type for a particular Helidon signal technology.
     * <p>>
     * Each telemetry signal technology (for example, tracing) can create one or more manifestations of that signal
     * (for example, a {@code io.helidon.tracing.Tracer}) using the {@code get} methods. Not all signals support versioning so
     * the for those signals the two {@code get} methods behave the same.
     *
     * @param <S> type of Helidon signal manifestation (e.g., {@code io.helidon.tracing.Tracer}) exposed by this signal
     */
    interface Signal<S> {

        /**
         * Returns an instance of the signal's Helidon signal manifestation.
         *
         * @param name name to assign to the new signal manifestation
         * @return new signal manifestation
         */
        S get(String name);

        /**
         * Returns an instance of the signal's Helidon signal manifestation.
         *
         * @param name name to assign to the new signal manifestation
         * @param version version to associate with the new signal manifestation
         * @return new signal manifestation
         */
        default S get(String name, String version) {
            return get(name);
        }

        /**
         * Reports the type of the signal this instance represents.
         *
         * @return signal type
         */
        Class<S> signalType();

        /**
         * Performs any clean-up related to the telemetry signal.
         */
        void close();

        /**
         * Unwraps the telemetry signal implementation as the specified type.
         *
         * @param type to which to convert
         *
         * @return the unwrapped value
         * @param <B> type to which to convert
         */
        <B> B unwrap(Class<B> type);
    }

    /**
     * Behavior common to all telemetry builder implementations.
     */
    interface Builder extends io.helidon.common.Builder<Builder, Telemetry> {

        /**
         * Sets the service name for the telemetry instance.
         *
         * @param service service name
         * @return updated builder
         */
        Builder service(String service);

        /**
         * Sets whether the telemetry instance should be enabled.
         *
         * @param enabled true to enable the telemetry instance; false otherwise
         * @return updated builder
         */
        Builder enabled(boolean enabled);


        /**
         * Sets whether the telemetry instance should be assigned as global.
         *
         * @param global true if the instance should be global; false otherwise
         * @return updated builder
         */
        Builder global(boolean global);

        /**
         * Sets the propagation types the telemetry instance should use.
         *
         * @param propagation propagation types to use
         * @return updated builder
         */
        Builder propagations(List<String> propagation);

        /**
         * Settings for use by telemetry implementations to aid in converting from neutral settings
         * to implementation-specific ones.
         */
        interface Blueprint {
            /**
             * Whether the telemetry instance should be set as the global isntance.
             *
             * @return true if the instance should be global; false otherwise
             */
            @Option.Configured
            @Option.DefaultBoolean(true)
            boolean global();

            /**
             * Whether telemetry should be enabled.
             *
             * @return true if enabled; false otherwise
             */
            @Option.Configured
            @Option.DefaultBoolean(true)
            boolean enabled();

            /**
             * Service name the telemetry instance should use in exporting data.
             *
             * @return service name
             */
            @Option.Configured
            @Option.Required
            String service();

            /**
             * The propagation types the telemetry instance should use.
             *
             * @return propagation types
             */
            @Option.Configured
            List<String> propagations();

        }
    }
}
