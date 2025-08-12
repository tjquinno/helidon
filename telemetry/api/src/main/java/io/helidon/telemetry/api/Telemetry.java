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

import java.util.Optional;

import io.helidon.common.config.NamedService;
import io.helidon.service.registry.Service;


/**
 * Contract for a telemetry implementation.
 * <p>
 * A telemetry implementation handles one or more <em>signals</em>&mdash;sources of telemetry&mdash;for example, tracing,
 * metrics, and logging.
 */
@Service.Contract
public interface Telemetry extends NamedService {

    /**
     * Config key for telemetry settings.
     */
    String CONFIG_KEY = "telemetry";

    /**
     * Returns the signal of the requested type, if present.
     *
     * @param signalType type of signal (e.g, {@code Tracing}
     * @return the {@link Telemetry.Signal} of the type
     * @param <T> type of the signal
     */
    <T> Optional<Signal<T>> signal(Class<T> signalType);

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
    }
}
