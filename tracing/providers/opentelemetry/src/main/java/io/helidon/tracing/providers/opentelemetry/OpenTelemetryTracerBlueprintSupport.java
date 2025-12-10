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
import java.util.StringJoiner;
import java.util.stream.Stream;

import io.helidon.builder.api.Prototype;
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

    private static final String DEFAULT_EXPORTER_SCHEME = "http";
    private static final String DEFAULT_EXPORTER_HOST = "localhost";
    private static final int DEFAULT_EXPORTER_PORT = 4317;

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryTracerBlueprintSupport.class.getName());

    private static final TextMapGetter GETTER = new Getter();
    private static final TextMapSetter SETTER = new Setter();

    private OpenTelemetryTracerBlueprintSupport() {
    }

    static class Decorator implements Prototype.BuilderDecorator<OpenTelemetryTracer.BuilderBase<?, ?>> {

        @Override
        public void decorate(OpenTelemetryTracer.BuilderBase<?, ?> target) {
            /*
            See the constructor of the manually-written (not generated) OpenTelemetryTracerImpl for some further
            initialization. It is done there because we want to wait for validation to run first before doing that work,
            and this decorator is invoked before validation.
             */

            addTypedTagsToTagMap(target);

            if (target.propagator().isEmpty()) {
                target.propagator(TextMapPropagator.composite(target.propagators()));
            }

            /*
            This method is invoked before the builder's values are validated. We need the service name so check it
            explicitly here.
             */
            String serviceName = target.serviceName()
                    .orElseThrow(() -> new IllegalStateException("Property \"service\" must not be null, but not set"));

            /*
            Set the openTelemetry and delegate if they were not explicitly assigned.
             */
            if (target.openTelemetry().isEmpty()) {
                target.openTelemetry(openTelemetryFromSettings(target));
            }
            if (target.delegate().isEmpty()) {
                target.delegate(target.openTelemetry().get().getTracer(serviceName));
            }

        }

        private static OpenTelemetry openTelemetryFromSettings(OpenTelemetryTracer.BuilderBase<?, ?> builder) {
            if (!builder.enabled()) {
                return OpenTelemetry.noop();
            }

            if (builder.openTelemetry().isPresent()) {
                return builder.openTelemetry().get();
            }

            var openTelemetrySdkBuilder = OpenTelemetrySdk.builder();

            var sdkTracerProviderBuilder = SdkTracerProvider.builder();

            var propagator = builder.propagator().orElse(TextMapPropagator.composite(builder.propagators()));
            openTelemetrySdkBuilder.setPropagators(ContextPropagators.create(propagator));

            spanProcessor(builder).ifPresent(sdkTracerProviderBuilder::addSpanProcessor);

            var attributesBuilder = Attributes.builder();
            attributesBuilder.put(ResourceAttributes.SERVICE_NAME, builder.serviceName().get());

            var resource = Resource.getDefault().merge(Resource.create(attributesBuilder.build()));
            sdkTracerProviderBuilder.setResource(resource);

            sampler(builder).ifPresent(sdkTracerProviderBuilder::setSampler);

            openTelemetrySdkBuilder.setTracerProvider(sdkTracerProviderBuilder.build());
            return openTelemetrySdkBuilder.build();
        }

        private static Optional<SpanProcessor> spanProcessor(OpenTelemetryTracer.BuilderBase<?, ?> builder) {

            var spanExporter = spanExporter(builder);
            return builder.spanProcessorType().map(spt -> switch (spt) {
                case SpanProcessorType.BATCH -> batchProcessor(builder, spanExporter);
                case SpanProcessorType.SIMPLE -> SimpleSpanProcessor.create(spanExporter);
            });
        }

        private static SpanExporter spanExporter(OpenTelemetryTracer.BuilderBase<?, ?> builder) {
            // The extended tracer settings do not expose an exporter type. Use OTLP via grpc.
            var spanExporterBuilder = OtlpGrpcSpanExporter.builder();
            StringBuilder exporterUrlBuilder = new StringBuilder();

            String scheme = builder.collectorPath().orElse(DEFAULT_EXPORTER_SCHEME);
            exporterUrlBuilder.append(scheme).append(scheme.endsWith(":") ? "" : ":").append("//");

            String host = builder.collectorHost().orElse(DEFAULT_EXPORTER_HOST);
            exporterUrlBuilder.append(host);

            int port = builder.collectorPort().orElse(DEFAULT_EXPORTER_PORT);
            exporterUrlBuilder.append(":").append(port);

            builder.collectorPath().ifPresent(path -> exporterUrlBuilder.append(path.startsWith("/") ? "" : "/").append(path));

            spanExporterBuilder.setEndpoint(exporterUrlBuilder.toString());
            if (builder.privateKey().isPresent() && builder.clientCertificate().isPresent()) {
                spanExporterBuilder.setClientTls(builder.privateKey().get().bytes(), builder.clientCertificate().get().bytes());
            }
            builder.trustedCertificate().ifPresent(certs -> spanExporterBuilder.setTrustedCertificates(certs.bytes()));

            return spanExporterBuilder.build();

        }

        private static SpanProcessor batchProcessor(OpenTelemetryTracer.BuilderBase<?, ?> builder, SpanExporter spanExporter) {
            var processorBuilder = BatchSpanProcessor.builder(spanExporter);
            builder.maxExportBatchSize().ifPresent(processorBuilder::setMaxExportBatchSize);
            builder.exportTimeout().ifPresent(processorBuilder::setExporterTimeout);
            builder.scheduleDelay().ifPresent(processorBuilder::setScheduleDelay);
            builder.maxQueueSize().ifPresent(processorBuilder::setMaxQueueSize);
            return processorBuilder.build();
        }

        private static Optional<Sampler> sampler(OpenTelemetryTracer.BuilderBase<?, ?> builder) {
            return builder.samplerType().map(st -> switch (st) {
                case SamplerType.CONSTANT -> Sampler.alwaysOn();
                case SamplerType.RATIO -> Sampler.traceIdRatioBased(builder.samplerParam().orElse(1.0d));
            });
        }

        private void addTypedTagsToTagMap(OpenTelemetryTracer.BuilderBase<?, ?> target) {
            target.tracerTags().forEach(target.tags()::put);
            target.intTracerTags().forEach((key, intValue) -> target.tags().put(key, intValue.toString()));
            target.booleanTracerTags().forEach((key, booleanValue) -> target.tags().put(key, booleanValue.toString()));
        }

    }

    static class CustomMethods {

        /**
         * Adds a string-valued tag.
         *
         * @param builder builder
         * @param name tag name
         * @param value tag value
         */
        @Prototype.BuilderMethod
        static void addTracerTag(OpenTelemetryTracer.BuilderBase<?, ?> builder, String name, String value) {
            builder.putTracerTag(name, value);
        }

        /**
         * Adds a numeric-valued tag.
         *
         * @param builder builder
         * @param name tag name
         * @param value tag value
         */
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

        /**
         * Adds a boolean-valued tag.
         *
         * @param builder builder
         * @param name tag name
         * @param value tag value
         */
        @Prototype.BuilderMethod
        static void addTracerTag(OpenTelemetryTracer.BuilderBase<?, ?> builder, String name, boolean value) {
            builder.putBooleanTracerTag(name, value);
        }

        /**
         * Adds a {@link io.helidon.tracing.SpanListener} to the builder for later registration with the resulting
         * {@link io.helidon.tracing.Tracer}.
         *
         * @param builder {@code Builder} to add the listener to
         * @param spanListener {@code SpanListener} to add to the {@code Tracer} built from the builder
         */
        @Prototype.BuilderMethod
        static void register(OpenTelemetryTracer.BuilderBase<?, ?> builder, SpanListener spanListener) {
            builder.spanListeners().add(spanListener);
        }

        /**
         * Registers a {@link io.helidon.tracing.SpanListener} with the tracer.
         *
         * @param openTelemetryTracer tracer with which to register the listener
         * @param spanListener the {@code SpanListener} to register
         * @return updated tracer
         */
        @Prototype.PrototypeMethod
        static Tracer register(OpenTelemetryTracer openTelemetryTracer, SpanListener spanListener) {
            openTelemetryTracer.spanListeners().add(spanListener);
            return openTelemetryTracer;
        }

        /**
         * Extract a {@link io.helidon.tracing.SpanContext} using headers and the propagator already associated with the
         * {@link io.helidon.tracing.Tracer}.
         *
         * @param openTelemetryTracer the {@code Tracer}
         * @param headersProvider     provider of headers (typically from an incoming request)
         * @return {@code SpanContext} if one is indicated; {@code Optional#empty} otherwise
         */
        @Prototype.PrototypeMethod
        public static Optional<SpanContext> extract(OpenTelemetryTracer openTelemetryTracer, HeaderProvider headersProvider) {
            Context context = openTelemetryTracer.propagator().extract(Context.current(), headersProvider, GETTER);

            return Optional.ofNullable(context)
                    .map(OpenTelemetrySpanContext::new);
        }

        /**
         * Inject the specified {@link io.helidon.tracing.SpanContext} into headers, using the propagator already associated
         * with the {@link io.helidon.tracing.Tracer}.
         *
         * @param openTelemetryTracer     the {@code Tracer}
         * @param spanContext             the {@code SpanContext} to inject
         * @param inboundHeadersProvider  provider of inbound headers (required by the signature but not used here)
         * @param outboundHeadersConsumer how the headers can be set to reflect the span context
         */
        @Prototype.PrototypeMethod
        public static void inject(OpenTelemetryTracer openTelemetryTracer,
                                  SpanContext spanContext,
                                  HeaderProvider inboundHeadersProvider,
                                  HeaderConsumer outboundHeadersConsumer) {
            openTelemetryTracer.propagator()
                    .inject(((OpenTelemetrySpanContext) spanContext).openTelemetry(), outboundHeadersConsumer, SETTER);
        }

        /**
         * Creates a {@link Span.Builder} for constructing a new {@link io.helidon.tracing.Span} from the specified {@link
         * io.helidon.tracing.Tracer} and assigning the name to be given to the span once built.
         *
         * @param openTelemetryTracer the {@code Tracer} from which to create the span builder
         * @param name span name to assign to the span once created
         * @return {@code Span.Builder}
         */
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
