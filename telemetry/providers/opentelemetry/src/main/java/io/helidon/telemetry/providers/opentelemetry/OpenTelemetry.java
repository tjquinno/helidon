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
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.telemetry.Telemetry;
import io.helidon.tracing.Tracer;

@RuntimeType.PrototypedBy(OpenTelemetryConfig.class)
public class OpenTelemetry implements Telemetry, RuntimeType.Api<OpenTelemetryConfig> {

    static OpenTelemetryConfig.Builder builder() {
        return OpenTelemetryConfig.builder();
    }

    static OpenTelemetry create(OpenTelemetryConfig config) {
        return new OpenTelemetry(config);
    }

    static OpenTelemetry create(Consumer<OpenTelemetryConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    private final OpenTelemetryConfig config;

    public OpenTelemetry(OpenTelemetryConfig config) {
        this.config = config;
    }


    @Override
    public String name() {
        return config.service();
    }

    @Override
    public String type() {
        return "otel";
    }

    @Override
    public OpenTelemetryConfig prototype() {
        return config;
    }

    @Override
    public void close() {
        config.signals().forEach(Telemetry.Signal::close);
    }

    @Override
    public <T> Optional<T> signal(Class<T> signalType) {
        return config.signals().stream()
                .filter(signalType::isInstance)
                .map(signalType::cast)
                .findFirst();
    }
}
