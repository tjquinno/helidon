/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.microprofile.telemetry;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration for telemetry.
 */
@Prototype.Blueprint
@Prototype.Configured(TelemetryConfigBlueprint.TELEMETRY_CONFIG_KEY)
interface TelemetryConfigBlueprint {

    /**
     * The config key containing settings for all of metrics.
     */
    String TELEMETRY_CONFIG_KEY = "telemetry";

    /**
     * Injection type for injected OpenTelemetry objects such as {@link io.opentelemetry.api.trace.Tracer} and
     * {@link io.opentelemetry.api.trace.Span}.
     *
     * @return injection type
     */
    @Option.Configured
    @Option.Default(InjectionType.DEFAULT)
    InjectionType injectionType();

}