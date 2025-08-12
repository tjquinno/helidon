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

import io.helidon.builder.api.Prototype;
import io.helidon.common.Errors;
import io.helidon.common.LazyValue;
import io.helidon.common.config.Config;

import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.samplers.Sampler;

class OpenTelemetryTracingConfigSupport {

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryTracingConfigSupport.class.getName());

    static class BuilderDecorator implements Prototype.BuilderDecorator<OpenTelemetryTracingConfig.BuilderBase<?, ?>> {

        private static final LazyValue<Sampler> DEFAULT_SAMPLER = LazyValue.create(() -> Sampler.parentBased(Sampler.alwaysOn()));

        @Override
        public void decorate(OpenTelemetryTracingConfig.BuilderBase<?, ?> target) {

            // Associate each processor with either its named exporter(s) or all exporters.

            Errors.Collector errorsCollector = Errors.collector();

            // Add configured processors to any the app added programmatically.
            target.addProcessors(target.processorConfigs().stream()
                                         .map(processorConfig -> OtelConfigSupport.createSpanProcessor(processorConfig,
                                                                                                       target.exporters(),
                                                                                                       errorsCollector))
                                         .toList());


//            OpenTelemetryTracingConfig tracerConfig = OpenTelemetryTracingConfig.create(config);
            //
            //            SdkTracerProviderBuilder builder = SdkTracerProvider.builder();
            //
            //            tracerConfig.sampler().ifPresent(builder::setSampler);
            //
            //            return builder.build();

            errorsCollector.collect().log(LOGGER);
        }

    }

    static class CustomMethods {

        @Prototype.FactoryMethod
        static SpanProcessorConfig createProcessorConfigs(Config config) {
            return OtelConfigSupport.createProcessorConfig(config);
        }

        @Prototype.FactoryMethod
        static Sampler createSampler(Config config) {
            return OtelConfigSupport.createSampler(config);
        }

        @Prototype.FactoryMethod
        static SpanLimits createSpanLimits(Config config) {
            return OtelConfigSupport.createSpanLimits(config);
        }

    }

}
