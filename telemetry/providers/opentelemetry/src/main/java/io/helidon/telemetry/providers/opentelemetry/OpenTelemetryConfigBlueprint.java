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

package io.helidon.telemetry.providers.opentelemetry;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.telemetry.TelemetryConfig;
import io.helidon.telemetry.providers.opentelemetry.spi.OpenTelemetrySignalProvider;
import io.helidon.telemetry.spi.TelemetryProvider;

import io.opentelemetry.context.propagation.TextMapPropagator;

/**
 * OpenTelemetry settings.
 */
@Prototype.Blueprint(decorator = OpenTelemetryConfigSupport.BuildDecorator.class)
@Prototype.Configured("telemetry")
@Prototype.CustomMethods(OpenTelemetryConfigSupport.CustomMethods.class)
@Prototype.Provides(TelemetryProvider.class)
//@Prototype.RegistrySupport
interface OpenTelemetryConfigBlueprint extends TelemetryConfig, Prototype.Factory<OpenTelemetry> {

    //    /**
    //     * Whether OpenTelemetry support is enabled.
    //     *
    //     * @return true if OTel is enabled; false otherwise
    //     */
    //    @Option.Configured
    //    @Option.DefaultBoolean(true)
    //    boolean enabled();
    //
    //    /**
    //     * Name of the telemetry service to use in registering with back ends.
    //     * @return telemetry service name
    //     */
    //    @Option.Configured
    //    @Option.Required
    //    String service();

    /**
     * Whether the {@link io.opentelemetry.api.OpenTelemetry} instance created from this configuration should be made the
     * global one.
     *
     * @return true if the configured instance should be made global; false otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean global();

    /**
     * OpenTelemetry {@link io.opentelemetry.context.propagation.TextMapPropagator} instances added explicitly by the app.
     * <p>
     * Default: {@value ContextPropagationType#DEFAULT_NAMES}.
     *
     * @return explicitly-added
     */
    @Option.Configured
    @Option.Singular
    @Option.DefaultCode("ContextPropagationType.DEFAULT_PROPAGATORS")
    List<TextMapPropagator> propagators();

    /**
     * OpenTelemetry signals (e.g., tracing) and, for each, signal-specific settings.
     *
     * @return signals
     */
    @SuppressWarnings("rawtypes")
    @Option.Configured
//    @Option.RegistryService
    @Option.Provider(value = OpenTelemetrySignalProvider.class)
    List<OpenTelemetrySignal> signals();

}
