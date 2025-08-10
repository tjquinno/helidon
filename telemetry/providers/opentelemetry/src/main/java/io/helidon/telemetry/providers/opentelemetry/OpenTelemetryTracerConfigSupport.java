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

import io.helidon.builder.api.Prototype;
import io.helidon.common.LazyValue;

import io.opentelemetry.sdk.trace.samplers.Sampler;

class OpenTelemetryTracerConfigSupport {

    static class BuilderDecorator implements Prototype.BuilderDecorator<OpenTelemetryTracerConfig.BuilderBase<?, ?>> {

        private static final LazyValue<Sampler> DEFAULT_SAMPLER = LazyValue.create(() -> Sampler.parentBased(Sampler.alwaysOn()));

        @Override
        public void decorate(OpenTelemetryTracerConfig.BuilderBase<?, ?> target) {
//            if (target.sampler().isEmpty()) {
//                target.sampler(sampler(target.samplerConfig()));
//            }
        }

//        private static Sampler sampler(Optional<SamplerConfig> samplerConfig) {
//            if (samplerConfig.isPresent()) {
//                var sc = samplerConfig.get();
//                return switch (sc.type()) {
//                    case SamplerType.ALWAYS_OFF -> Sampler.alwaysOff();
//                    case SamplerType.ALWAYS_ON -> Sampler.alwaysOn();
//                    case SamplerType.PARENT_BASED_ALWAYS_OFF -> Sampler.parentBased(Sampler.alwaysOff());
//                    case SamplerType.PARENT_BASED_ALWAYS_ON -> Sampler.parentBased(Sampler.alwaysOn());
//                    case SamplerType.PARENT_BASED_TRACE_ID_RATIO -> Sampler.parentBased(Sampler.traceIdRatioBased(sc.param()
//                                                                                                                          .map(Number::doubleValue)
//                                                                                                                          .orElseThrow()));
//                    case SamplerType.TRACE_ID_RATIO -> Sampler.traceIdRatioBased(sc.param()
//                                                                                         .map(Number::doubleValue)
//                                                                                         .orElseThrow());
//                };
//            }
//
//            return DEFAULT_SAMPLER.get();
//        }
    }

    static class SamplerConfigDecorator implements Prototype.OptionDecorator<OpenTelemetryTracerConfig.BuilderBase<?, ?>, Optional<SamplerConfig>> {

        @Override
        public void decorate(OpenTelemetryTracerConfig.BuilderBase<?, ?> builder, Optional<SamplerConfig> samplerConfig) {

//            builder.sampler(samplerConfig.sampler());
        }
    }
}
