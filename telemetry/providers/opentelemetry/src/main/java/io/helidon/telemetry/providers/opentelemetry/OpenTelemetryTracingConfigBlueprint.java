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

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.telemetry.providers.opentelemetry.spi.OpenTelemetrySignalProvider;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * OpenTelemetry tracer settings.
 */
@Prototype.Configured(value = OpenTelemetryTracing.TYPE, root = false)
@Prototype.Blueprint(decorator = OpenTelemetryTracingConfigSupport.BuilderDecorator.class)
@Prototype.Provides(OpenTelemetrySignalProvider.class)
interface OpenTelemetryTracingConfigBlueprint extends Prototype.Factory<OpenTelemetryTracing> {

    @Prototype.FactoryMethod
    static Sampler createSampler(SamplerConfig samplerConfig) {

        return switch (samplerConfig.type()) {
            case SamplerType.ALWAYS_OFF -> Sampler.alwaysOff();
            case SamplerType.ALWAYS_ON -> Sampler.alwaysOn();
            case SamplerType.PARENT_BASED_ALWAYS_OFF -> Sampler.parentBased(Sampler.alwaysOff());
            case SamplerType.PARENT_BASED_ALWAYS_ON -> Sampler.parentBased(Sampler.alwaysOn());
            case SamplerType.PARENT_BASED_TRACE_ID_RATIO -> Sampler.parentBased(
                    Sampler.traceIdRatioBased(samplerConfig.param()
                                                      .map(Number::doubleValue)
                                                      .orElseThrow()));
            case SamplerType.TRACE_ID_RATIO -> Sampler.traceIdRatioBased(samplerConfig.param()
                                                                                 .map(Number::doubleValue)
                                                                                 .orElseThrow());
        };
    }

    @Prototype.FactoryMethod
    static SpanLimits createSpanLimits(SpanLimitsConfig config) {
        var builder = SpanLimits.builder();

        config.maxAttributes().ifPresent(builder::setMaxNumberOfAttributes);
        config.maxAttributeValueLength().ifPresent(builder::setMaxAttributeValueLength);
        config.maxEvents().ifPresent(builder::setMaxNumberOfEvents);
        config.maxLinks().ifPresent(builder::setMaxNumberOfLinks);
        config.maxAttributeValueLength().ifPresent(builder::setMaxAttributeValueLength);

        return builder.build();
    }

    /**
     * Name of this instance.
     *
     * @return name of the instance
     */
    @Option.Default(OpenTelemetryTracing.TYPE)
    String name();

    /**
     * Tracing sampler
     *
     * @return tracing sampler
     */
    @Option.Configured()
    Optional<Sampler> sampler();

    /**
     * Tracing span limits.
     *
     * @return tracing span limits
     */
    @Option.Configured
    Optional<SpanLimits> spanLimits();

    /**
     * OTel tracer provider prepared using these configuration settings.
     *
     * @return tracer provider
     */
    @Option.Access("")
    Optional<SdkTracerProvider> tracerProvider();

}
