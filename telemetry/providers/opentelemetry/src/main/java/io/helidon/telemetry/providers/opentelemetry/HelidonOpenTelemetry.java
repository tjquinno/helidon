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
import io.helidon.common.config.NamedService;
import io.helidon.telemetry.api.Telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;

/**
 * Implementation of the {@link io.helidon.telemetry.api.Telemetry} interface for OpenTelemetry.
 */
@RuntimeType.PrototypedBy(OpenTelemetryConfig.class)
public class HelidonOpenTelemetry implements Telemetry, RuntimeType.Api<OpenTelemetryConfig> {

    /*
    We se the type name HelidonOpenTelemetry instead of OpenTelemetry just to make things clearer for people reading the code, to
    help distinguish this type from the actual OpenTelemetry "OpenTelemetry" type.
     */

    static final String TYPE = "otel";

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetry.class.getName());
    private final OpenTelemetryConfig config;

    HelidonOpenTelemetry(OpenTelemetryConfig config) {
        this.config = config;
    }

    static OpenTelemetryConfig.Builder builder() {
        return OpenTelemetryConfig.builder();
    }

    static HelidonOpenTelemetry create(OpenTelemetryConfig config) {
        return new HelidonOpenTelemetry(config);
    }

    static HelidonOpenTelemetry create(Consumer<OpenTelemetryConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    @Override
    public OpenTelemetryConfig prototype() {
        return config;
    }

    @Override
    public void close() {
        config.signals().forEach(Telemetry.Signal::close);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<Telemetry.Signal<T>> signal(Class<T> signalType) {
        return config.signals().stream()
                .filter(signal -> signalType.isAssignableFrom(signal.signalType()))
                .map(signal -> (Telemetry.Signal<T>) signal)
                .findFirst();
    }

    @Override
    public <B> B unwrap(Class<B> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (type.isInstance(config.openTelemetry())) {
            return type.cast(config.openTelemetry());
        }
        throw new IllegalArgumentException("Cannot unwrap this instance of type " + this.getClass().getName()
        + " to the requested type " + type.getName());
    }

    /**
     * Behavior of each OpenTelemetry signal implementation in Helidon.
     *
     * @param <S> type of signal origin (e.g., tracer) furnished by this signal
     */
    public interface Signal<S> extends Telemetry.Signal<S>, NamedService {

        /**
         * Applies the signal information to the provided SDK builder, typically invoking a method to assign a signal provider
         * such as
         * {@link io.opentelemetry.sdk.OpenTelemetrySdkBuilder#setTracerProvider(io.opentelemetry.sdk.trace.SdkTracerProvider)}.
         *
         * @param sdkBuilder OpenTelemetry SDK builder to apply the signal information to
         */
        void update(OpenTelemetrySdkBuilder sdkBuilder);

        /**
         * Performs any follow-up work needed for this signal using the built (or previously-assigned global)
         * {@link io.opentelemetry.api.OpenTelemetry} object.
         *
         * @param openTelemetry the {@code OpenTelemetry} instance the signal should use
         */
        void openTelemetry(io.opentelemetry.api.OpenTelemetry openTelemetry);

        /**
         * Performs any clean-up as part of shutting down telemetry.
         */
        void close();
    }

}
