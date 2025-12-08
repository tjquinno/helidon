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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.stream.Stream;

import io.helidon.builder.api.Prototype;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.config.Config;
import io.helidon.tracing.HeaderConsumer;
import io.helidon.tracing.HeaderProvider;
import io.helidon.tracing.SamplerType;
import io.helidon.tracing.Span;
import io.helidon.tracing.SpanContext;
import io.helidon.tracing.SpanListener;
import io.helidon.tracing.SpanProcessorType;
import io.helidon.tracing.Tracer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

class OpenTelemetryTracerBlueprintSupport {

    static final String PROPAGATORS_DEFAULT = "new java.util.ArrayList<>(io.helidon.tracing.providers.opentelemetry"
            + ".ContextPropagationType.DEFAULT_PROPAGATORS)";
    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryTracerBlueprintSupport.class.getName());

    private static final TextMapGetter GETTER = new Getter();
    private static final TextMapSetter SETTER = new Setter();

    static class Decorator implements Prototype.BuilderDecorator<OpenTelemetryTracer.BuilderBase<?, ?>> {

        @Override
        public void decorate(OpenTelemetryTracer.BuilderBase<?, ?> target) {
            /*
            See the constructor of the manually-written (not generated) OpenTelemetryTracerImpl for some further
            initialization. It is done there because we want to wait for validation to run first before doing that work,
            and this decorator is invoked before validation.
             */

            addTypedTagsToTagMap(target);
        }

        private void addTypedTagsToTagMap(OpenTelemetryTracer.BuilderBase<?, ?>target) {
            target.tracerTags().forEach(target.tags()::put);
            target.intTracerTags().forEach((key, intValue) -> target.tags().put(key, intValue.toString()));
            target.booleanTracerTags().forEach((key, booleanValue) -> target.tags().put(key, booleanValue.toString()));
        }

    }

    static class CustomMethods {

        @Prototype.BuilderMethod
        static void addTracerTag(OpenTelemetryTracer.BuilderBase<?, ?> builder, String name, String value) {
            builder.putTracerTag(name, value);
        }

        @Prototype.BuilderMethod
        static void addTracerTag(OpenTelemetryTracer.BuilderBase<?, ?> builder, String name, Number value) {
            int intValue = value.intValue();
            if (value.doubleValue() % 1 != 0) {
                LOGGER.log(System.Logger.Level.WARNING, "Value for tag $0 of $1 should be an integer; converting to $2",
                           name,
                           intValue);
            }
            builder.putIntTracerTag(name, intValue);
        }

        @Prototype.BuilderMethod
        static void addTracerTag(OpenTelemetryTracer.BuilderBase<?, ?> builder, String name, boolean value) {
            builder.putBooleanTracerTag(name, value);
        }

        @Prototype.BuilderMethod
        static void register(OpenTelemetryTracer.BuilderBase<?, ?> builder, SpanListener spanListener) {
            builder.spanListeners().add(spanListener);
        }

        @Prototype.PrototypeMethod
        static Tracer register(OpenTelemetryTracer openTelemetryTracer, SpanListener spanListener) {
            openTelemetryTracer.spanListeners().add(spanListener);
            return openTelemetryTracer;
        }

        @Prototype.PrototypeMethod
        public static Optional<SpanContext> extract(OpenTelemetryTracer openTelemetryTracer, HeaderProvider headersProvider) {
            Context context = openTelemetryTracer.propagator().extract(Context.current(), headersProvider, GETTER);

            return Optional.ofNullable(context)
                    .map(OpenTelemetrySpanContext::new);
        }

        @Prototype.PrototypeMethod
        public static void inject(OpenTelemetryTracer openTelemetryTracer,
                                  SpanContext spanContext,
                                  HeaderProvider inboundHeadersProvider,
                                  HeaderConsumer outboundHeadersConsumer) {
            openTelemetryTracer.propagator()
                    .inject(((OpenTelemetrySpanContext) spanContext).openTelemetry(), outboundHeadersConsumer, SETTER);
        }

        @Prototype.PrototypeMethod
        public static Span.Builder<?> spanBuilder(OpenTelemetryTracer openTelemetryTracer, String name) {
            OpenTelemetrySpanBuilder builder = new OpenTelemetrySpanBuilder(openTelemetryTracer.delegate().spanBuilder(name),
                                                                            openTelemetryTracer.spanListeners());
            Span.current().map(Span::context).ifPresent(builder::parent);
            openTelemetryTracer.tags().forEach(builder::tag);
            return builder;
        }

        /**
         * Converts a config node for propagators into a list of {@link io.opentelemetry.context.propagation.TextMapPropagator}.
         * <p>
         * As a user convenience, the config node can be either a node list (in which case each node's string value will be
         * used for a propagator name) or the node can be a single string containing a comma-separated list of propagator names.
         *
         * @param config config node (node list of string nodes or a single node)
         * @return list of selected propagators
         */
        @Prototype.FactoryMethod
        static List<TextMapPropagator> createPropagators(Config config) {

            Stream<String> propagatorNames = config.isList()
                    ? config.asList(String.class).get().stream()
                    : Arrays.stream(config.asString().get().split(","));

            return propagatorNames
                    .map(ContextPropagationType::from)
                    .map(ContextPropagationType::propagator)
                    .toList();
        }
    }

    private static class Getter implements TextMapGetter<HeaderProvider> {
        @Override
        public Iterable<String> keys(HeaderProvider headerProvider) {
            return headerProvider.keys();
        }

        @Override
        public String get(HeaderProvider headerProvider, String s) {
            StringJoiner joiner = new StringJoiner(",").setEmptyValue("");
            headerProvider.getAll(s).forEach(joiner::add);
            return joiner.toString();
        }
    }

    private static class Setter implements TextMapSetter<HeaderConsumer> {
        @Override
        public void set(HeaderConsumer carrier, String key, String value) {
            carrier.set(key, value);
        }
    }
}
