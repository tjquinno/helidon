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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.config.Config;
import io.helidon.tracing.SamplerType;
import io.helidon.tracing.SpanListener;
import io.helidon.tracing.SpanProcessorType;
import io.helidon.tracing.Tracer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
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

class OpenTelemetryTracerImpl extends OpenTelemetryTracer.BuilderBase.OpenTelemetryTracerImpl implements Tracer {

    private static final String DEFAULT_EXPORTER_SCHEME = "http";
    private static final String DEFAULT_EXPORTER_HOST = "localhost";
    private static final int DEFAULT_EXPORTER_PORT = 4317;

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryTracerImpl.class.getName());

    private final List<SpanListener> spanListeners = new ArrayList<>();

    static OpenTelemetryTracerImpl create(Config config) {
        return OpenTelemetryTracerBuilder.create()
                .config(config)
                .build();
    }

    /**
     * Create an instance providing a builder.
     *
     * @param builder extending builder base of this prototype
     */
    protected OpenTelemetryTracerImpl(BuilderBase<?, ?> builder) {
        super(adjustedBuilder(builder));

        /*
        Some Helidon code that uses the builder will have directly assigned the OpenTelemetry object, the OpenTelemetry Tracer
        object (delegate), and a map of attributes.

        If those values were not set explicitly, then set them using config information from the builder.

        The builder's validation makes sure that either both of those objects were set or neither was.
         */

        var openTelemetry = builder.openTelemetry().orElse(openTelemetryFromSettings(builder));

        var openTelemetryTracer = builder.delegate().orElse(openTelemetry.getTracer(serviceName()));

        if (enabled() && registerGlobal()) {
            try {
                GlobalOpenTelemetry.set(openTelemetry);
                var globalHelidonTracer = HelidonOpenTelemetry.create(openTelemetry,
                                                                      openTelemetryTracer,
                                                                      Map.of());
                Tracer.global(globalHelidonTracer);
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to set global OpenTelemetry as requested by tracing settings", e);
            }
        }

        spanListeners.addAll(HelidonServiceLoader.create(ServiceLoader.load(SpanListener.class)).asList());
        spanListeners.addAll(builder.spanListeners());

    }

    @Override
    public Tracer register(SpanListener spanListener) {
        spanListeners.add(spanListener);
        return this;
    }

    @Override
    public List<SpanListener> spanListeners() {
        return Collections.unmodifiableList(spanListeners);
    }

    private static BuilderBase<?, ?> adjustedBuilder(BuilderBase<?, ?> builder) {
        if (builder.openTelemetry().isEmpty()) {
            builder.openTelemetry(openTelemetryFromSettings(builder));
        }

        if (builder.delegate().isEmpty()) {
            builder.delegate(builder.openTelemetry().get().getTracer(builder.serviceName().get()));
        }
        return builder;
    }

    private static OpenTelemetry openTelemetryFromSettings(BuilderBase<?, ?> builder) {
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

    private static Optional<SpanProcessor> spanProcessor(BuilderBase<?, ?> builder) {

        var spanExporter = spanExporter(builder);
        return builder.spanProcessorType().map(spt -> switch (spt) {
            case SpanProcessorType.BATCH -> batchProcessor(builder, spanExporter);
            case SpanProcessorType.SIMPLE -> SimpleSpanProcessor.create(spanExporter);
        });
    }

    private static SpanExporter spanExporter(BuilderBase<?, ?> builder) {
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

    private static SpanProcessor batchProcessor(BuilderBase<?, ?> builder, SpanExporter spanExporter) {
        var processorBuilder = BatchSpanProcessor.builder(spanExporter);
        builder.maxExportBatchSize().ifPresent(processorBuilder::setMaxExportBatchSize);
        builder.exportTimeout().ifPresent(processorBuilder::setExporterTimeout);
        builder.scheduleDelay().ifPresent(processorBuilder::setScheduleDelay);
        builder.maxQueueSize().ifPresent(processorBuilder::setMaxQueueSize);
        return processorBuilder.build();
    }

    private static Optional<Sampler> sampler(BuilderBase<?, ?> builder) {
        return builder.samplerType().map(st -> switch (st) {
            case SamplerType.CONSTANT -> Sampler.alwaysOn();
            case SamplerType.RATIO -> Sampler.traceIdRatioBased(builder.samplerParam().orElse(1.0d));
        });
    }

}
