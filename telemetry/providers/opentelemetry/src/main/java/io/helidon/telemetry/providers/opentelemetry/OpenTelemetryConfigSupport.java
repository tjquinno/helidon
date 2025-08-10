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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;
import io.helidon.common.config.ConfigValue;
import io.helidon.tracing.Tracer;

import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

class OpenTelemetryConfigSupport {

    static class BuildDecorator implements Prototype.BuilderDecorator<OpenTelemetryConfig.BuilderBase<?, ?>> {

        @Override
        public void decorate(OpenTelemetryConfig.BuilderBase<?, ?> target) {


            OpenTelemetrySdkBuilder openTelemetrySdkBuilder = OpenTelemetrySdk.builder();

            if (!target.propagators().isEmpty()) {
                openTelemetrySdkBuilder.setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(target.propagators())));
            }

            target.tracerProvider()
                    .filter(tracerProvider -> tracerProvider instanceof SdkTracerProvider sdkTracerProvider)
                    .map(tracerProvider -> (SdkTracerProvider) tracerProvider)
                    .ifPresent(openTelemetrySdkBuilder::setTracerProvider);

            openTelemetrySdkBuilder.build();
        }

    }


    static class CustomMethods {

        /**
         * Converts a config node for propagators into a list of {@link io.opentelemetry.context.propagation.TextMapPropagator}.
         * <p>
         * The config node can be either a node list (in which case each node's string value will be used for a propagator name)
         * or the node can be a single string containing a comma-separated list of propagator names.
         *
         * @param config config node (node list of string nodes or a single node)
         * @return list of selected propagators
         */
        @Prototype.FactoryMethod
        static List<TextMapPropagator> createPropagators(Config config) {
            Stream<String> propagatorNames = config.isList()
                    ? config.asNodeList()
                    .map(nodeList -> nodeList.stream()
                            .map(Config::asString)
                            .filter(ConfigValue::isPresent)
                            .map(ConfigValue::get))
                    .orElse(Stream.empty())
                    : Arrays.stream(config.asString().get().split(","));

            return propagatorNames
                    .map(ContextPropagationType::from)
                    .map(ContextPropagationType::propagator)
                    .toList();
        }

        /**
         * Creates an OpenTelemetry {@link io.opentelemetry.api.trace.TracerProvider} from the
         * tracer configuration.
         *
         * @param config tracer configuration
         * @return OTel tracer provider
         */
        @Prototype.FactoryMethod
        static TracerProvider createTracerProvider(Config config) {
            OpenTelemetryTracerConfig tracerConfig = OpenTelemetryTracerConfig.create(config);

            SdkTracerProviderBuilder builder = SdkTracerProvider.builder();

            tracerConfig.sampler().ifPresent(builder::setSampler);

            return builder.build();
        }

//        @Prototype.PrototypeMethod
//        static Tracer tracer(OpenTelemetryConfig prototype) {
//
//        }


    }

}

