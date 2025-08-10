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

package io.helidon.telemetry;

import java.util.Optional;

import io.helidon.common.config.NamedService;
import io.helidon.service.registry.Service;

/**
 * Contract for a telemetry implementation.
 */
@Service.Contract
public interface Telemetry extends NamedService {

//    /**
//     * Provides the {@link io.helidon.tracing.Tracer} initialized in this telemetry instance.
//     *
//     * @return tracer
//     */
//    Tracer tracer();

    /**
     * Returns the signal of the requested type, if present.
     *
     * @param signalType type of signal (e.g, {@code Tracing}
     * @return the {@link io.helidon.telemetry.Telemetry.Signal} of the type
     * @param <T> type of the signal
     */
    <T> Optional<T> signal(Class<T> signalType);

    /**
     * Shuts down telemetry.
     */
    void close();

    /**
     * Abstraction of a telemetry signal type for a particular Helidon signal technology (e.g., tracing).
     *
     * @param <T> type of Helidon signal manifestation (e.g., {@code io.helidon.tracing.Tracer}) exposed by this signal
     */
    interface Signal<T> {

        /**
         * Returns an instance of the signal's Helidon signal manifestation.
         *
         * @param name name to assign to the new signal manifestation
         * @return new signal manifestation
         */
        T get(String name);

        /**
         * Returns an instance of the signal's Helidon signal manifestation.
         *
         * @param name name to assign to the new signal manifestation
         * @param version version to associate with the new signal manifestation
         * @return new signal manifestation
         */
        default T get(String name, String version) {
            return get(name);
        }

        /**
         * Performs any clean-up related to the telemetry signal.
         */
        void close();
    }
}
