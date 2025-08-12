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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.config.NamedService;
import io.helidon.service.registry.Service;
import io.helidon.telemetry.api.Telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;

/**
 * Implementation of the {@link io.helidon.telemetry.api.Telemetry} interface for OpenTelemetry.
 */
//@Service.Singleton
@RuntimeType.PrototypedBy(OpenTelemetryConfig.class)
public class OpenTelemetry implements Telemetry, RuntimeType.Api<OpenTelemetryConfig> {

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetry.class.getName());
    private final OpenTelemetryConfig config;

    OpenTelemetry(OpenTelemetryConfig config) {
        this.config = config;
    }

    static OpenTelemetryConfig.Builder builder() {
        return OpenTelemetryConfig.builder();
    }

    static OpenTelemetry create(OpenTelemetryConfig config) {
        return new OpenTelemetry(config);
    }

    static OpenTelemetry create(Consumer<OpenTelemetryConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    @Service.PostConstruct
    void init() {
        AtomicReference<io.opentelemetry.api.OpenTelemetry> otToUse = new AtomicReference<>(prototype().openTelemetry()
                                                                                                    .orElse(prototype().openTelemetrySdk()));

        if (prototype().global()) {
            List<String> otelReasonsForUsingAutoConfig = otelReasonsForUsingAutoConfig();
            if (!otelReasonsForUsingAutoConfig.isEmpty()) {
                if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                    LOGGER.log(System.Logger.Level.TRACE,
                               "Using OTel autoconfigure: " + otelReasonsForUsingAutoConfig);
                }
                otToUse.set(GlobalOpenTelemetry.get());

            } else {
                GlobalOpenTelemetry.set(otToUse.get());
            }

            // Either there was already an existing global OpenTelemetry or we just set one. In either case,
            // register the global OpenTelemetry in the Helidon service registry.
            //                Services.set(OpenTelemetry.class, otToRegister);
        }
        prototype().signals().forEach(s -> s.openTelemetry(otToUse.get()));
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<Telemetry.Signal<T>> signal(Class<T> signalType) {
        return config.signals().stream()
                .filter(signal -> signalType.isAssignableFrom(signal.signalType()))
                .map(signal -> (Telemetry.Signal<T>) signal)
                .findFirst();
    }

    static List<String> otelReasonsForUsingAutoConfig() {
        List<String> reasons = new ArrayList<>();
        if (Boolean.getBoolean("otel.java.global-autoconfigure.enabled")) {
            reasons.add("OpenTelemetry global autoconfigure is enabled using otel.java.global-autoconfigure.enabled");
        }
        String envvar = System.getenv("OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED");
        if (envvar != null && envvar.equals("true")) {
            reasons.add("OpenTelemetry global autoconfigure is enabled using OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED");
        }
        return reasons;
    }
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
         * Performs any follow-up work needed for this signal using the built (or previoysly-assigned global)
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
