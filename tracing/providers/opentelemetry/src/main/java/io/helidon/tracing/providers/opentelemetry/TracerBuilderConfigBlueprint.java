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

package io.helidon.tracing.providers.opentelemetry;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Tracer settings, largely compatible with the Jaeger and Zipkin tracer-centric configuration formats anchored at the top-level
 * configuration key {@value CONFIG_KEY}.
 * <p>
 * Users employing OpenTelemetry as the tracing implementation are strongly encouraged to use telemetry-centric
 * configuration anchored at the top-level configuration key {@value io.helidon.telemetry.api.Telemetry#CONFIG_KEY}.
 */
@Prototype.Configured(TracerBuilderConfigBlueprint.CONFIG_KEY)
@Prototype.Blueprint
@Prototype.CustomMethods(TracerBuilderConfigSupport.CustomMethods.class)
interface TracerBuilderConfigBlueprint {

    /**
     * Top-level config key for these settings.
     */
    String CONFIG_KEY = "tracing";

    @Option.Configured("service")
    String serviceName();

    @Option.Configured
    Optional<String> global();

    @Option.Configured
    Optional<String> collectorProtocol();

    @Option.Configured
    Optional<String> collectorPort();

    @Option.Configured
    Optional<String> collectorPath();

    @Option.Configured
    Optional<String> collectorHost();

    @Option.Configured
    Optional<String> clientCertPem();

    @Option.Configured
    Optional<String> exporterTimeout();

    @Option.Configured
    Optional<String> maxExportBatchSize();

    @Option.Configured
    Optional<String> maxQueueSize();

    @Option.Configured
    Optional<String> privateKeyPem();

    @Option.Configured
    Optional<String> propagation();

    @Option.Configured
    Optional<String> samplerParam();

    @Option.Configured
    Optional<String> samplerType();

    @Option.Configured
    Optional<String> scheduleDelay();

    @Option.Configured
    Optional<String> spanProcessorType();

    @Option.Configured
    Optional<String> trustedCertPem();

}
