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
import io.helidon.common.config.Config;
import io.helidon.telemetry.TelemetryTracerConfig;
import io.helidon.tracing.spi.TracerProvider;

import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * OpenTelemetry tracer settings.
 */
@Prototype.Configured
@Prototype.Blueprint(decorator = OpenTelemetryTracerConfigSupport.BuilderDecorator.class)
//@Prototype.Provides(TracerProvider.class)
interface OpenTelemetryTracerConfigBlueprint extends TelemetryTracerConfig {

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

//    @Prototype.FactoryMethod
//    static Sampler createSampler(Config config) {
//        SamplerConfig samplerConfig = SamplerConfig.create(config);
//
//    }

    /**
     * Tracing sampler
     *
     * @return tracing sampler
     */
    @Option.Configured()
//    @Option.Decorator(OpenTelemetryTracerConfigSupport.SamplerConfigDecorator.class)
    Optional<Sampler> sampler();

}
