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

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.telemetry.spi.TelemetryProvider;
import io.helidon.tracing.Tracer;

/**
 * Common configuration settings for telemetry.
 */
@Prototype.Configured(value = "telemetry")
@Prototype.Blueprint
interface TelemetryConfigBlueprint /* extends Prototype.Factory<Telemetry> */ {

    /**
     * Telemetry service name reported to back ends.
     *
     * @return service name
     */
    @Option.Configured
    String service();

    /**
     * Whether telemetry is enabled.
     *
     * @return true if telemetry is enabled; false otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * The tracer used by telemetry.
     *
     * @return tracer
     */
    @Option.Configured
    Optional<Tracer> tracer();

}
