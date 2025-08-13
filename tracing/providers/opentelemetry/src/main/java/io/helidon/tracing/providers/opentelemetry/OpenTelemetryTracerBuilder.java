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

import java.util.HashMap;
import java.util.Map;

import io.helidon.common.LazyValue;
import io.helidon.common.config.Config;
import io.helidon.service.registry.Services;
import io.helidon.telemetry.api.Telemetry;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.TracerBuilder;

//?? need to fix all the nulls!
/**
 * Tracer builder based on OpenTelemetry.
 */
class OpenTelemetryTracerBuilder implements TracerBuilder<OpenTelemetryTracerBuilder> {

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_GLOBAL = true;

    private LazyValue<Telemetry> telemetry = LazyValue.create(() -> Services.get(Telemetry.class));

    private String serviceName;

    // exporter (collector) settings
    private String scheme;
    private int port;
    private String host;
    private String path;

    private final Map<String, String> tracerTags = new HashMap<>();

    private Config topLevelTracingConfig;

    private boolean enabled = DEFAULT_ENABLED;
    private boolean global = DEFAULT_GLOBAL;

    @Override
    public OpenTelemetryTracerBuilder serviceName(String name) {
        this.serviceName = name;
        return this;
    }

    @Override
    public OpenTelemetryTracerBuilder collectorProtocol(String protocol) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder collectorPort(int port) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder collectorHost(String host) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder collectorPath(String path) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder addTracerTag(String key, String value) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder addTracerTag(String key, Number value) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder addTracerTag(String key, boolean value) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder config(Config config) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder enabled(boolean enabled) {
        return null;
    }

    @Override
    public OpenTelemetryTracerBuilder registerGlobal(boolean global) {
        return null;
    }

    @Override
    public <B> B unwrap(Class<B> builderClass) {
        return null;
    }

    @Override
    public Tracer build() {
        return null;
    }
}
