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
        super(builder);

        if (enabled() && registerGlobal()) {
            try {
                GlobalOpenTelemetry.set(openTelemetry());
                var globalHelidonTracer = HelidonOpenTelemetry.create(openTelemetry(),
                                                                      delegate(),
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

    @Override
    public <T> T unwrap(Class<T> tracerClass) {
        if (tracerClass.isInstance(delegate())) {
            return tracerClass.cast(delegate());
        }
        return super.unwrap(tracerClass);
    }
}
