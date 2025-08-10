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

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.config.Config;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.providers.opentelemetry.OpenTelemetryTracerProvider;

import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

@RuntimeType.PrototypedBy(OpenTelemetryTracingConfig.class)
class OpenTelemetryTracing implements OpenTelemetrySignal<Tracer>, RuntimeType.Api<OpenTelemetryTracingConfig> {

    static final String TYPE = "tracing";

    public static OpenTelemetryTracingConfig.Builder builder() {
        return OpenTelemetryTracingConfig.builder();
    }

    public static OpenTelemetryTracing create() {
        return builder().build();
    }

    public static OpenTelemetryTracing create(Config config) {
        return builder()
                .config(config)
                .build();
    }

    public static OpenTelemetryTracing create(Consumer<OpenTelemetryTracingConfig.Builder> configurator) {
        return builder()
                .update(configurator)
                .build();
    }

    public static OpenTelemetryTracing create(OpenTelemetryTracingConfig config) {
        return new OpenTelemetryTracing(config);
    }

    private final OpenTelemetryTracingConfig config;
    private final SdkTracerProvider sdkTracerProvider;

    private TracerProvider otelTracerProvider;

    private OpenTelemetryTracing(OpenTelemetryTracingConfig config) {
        this.config = config;
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder();

        config.sampler().ifPresent(builder::setSampler);

        sdkTracerProvider = builder.build();
    }

    @Override
    public String name() {
        return "tracing";
    }

    @Override
    public String type() {
        return "tracing";
    }

    @Override
    public OpenTelemetryTracingConfig prototype() {
        return config;
    }

    @Override
    public void update(OpenTelemetrySdkBuilder sdkBuilder) {
        sdkBuilder.setTracerProvider(sdkTracerProvider);
    }

    @Override
    public void processSdk(OpenTelemetrySdk sdk) {
        this.otelTracerProvider = sdk.getTracerProvider();
    }

    @Override
    public void close() {

    }

    @Override
    public Tracer get(String name) {
        return OpenTelemetryTracerProvider.tracer(otelTracerProvider.get(name));
    }
}
