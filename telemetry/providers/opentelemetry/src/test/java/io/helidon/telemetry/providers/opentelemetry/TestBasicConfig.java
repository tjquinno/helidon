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

import io.helidon.common.concurrency.limits.Limit;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.common.testing.junit5.OptionalMatcher;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.telemetry.api.Telemetry;
import io.helidon.testing.junit5.Testing;
import io.helidon.tracing.Tracer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class TestBasicConfig {

//    @BeforeAll
//    static void setup() {
//        ServiceRegistryManager.start();
//    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[\"tracecontext\",\"baggage\",\"jaeger\"]", // list of strings--a node list
            "tracecontext,baggage,jaeger"})
        // one node with a comma-separated list
    void testSimpleConfig(String propagatorsValue) {

        Config config = Config.just(ConfigSources
                                            .create(String.format("""
                                                                          telemetry:
                                                                            service: "test-otel"
                                                                            enabled: false
                                                                            global: false
                                                                            propagators: %s
                                                                          """, propagatorsValue),
                                                    MediaTypes.APPLICATION_YAML));

        OpenTelemetry openTelemetry = OpenTelemetry.builder().config(config.get("telemetry")).build();

        assertThat("Helidon OpenTelemetry", openTelemetry, notNullValue());
        assertThat("Propagators",
                   openTelemetry.prototype().propagators(),
                   hasItems(instanceOf(W3CBaggagePropagator.class),
                            instanceOf(W3CTraceContextPropagator.class),
                            instanceOf(JaegerPropagator.class)));

        assertThat("Service name", openTelemetry.prototype().service(), is("test-otel"));
        assertThat("Enabled", openTelemetry.prototype().enabled(), is(false));
        assertThat("Global", openTelemetry.prototype().global(), is(false));
    }

    @Test
    void testCommaListPropagators() {

    }

    @Test
    void testSamplerConfig() {
        Config config = Config.just(ConfigSources
                                            .create("""
                                                            tracer:
                                                              sampler:
                                                                type: "always_on"
                                                            """,
                                                    MediaTypes.APPLICATION_YAML));

                OpenTelemetryTracing otelTracing = OpenTelemetryTracingConfig.builder()
                        .config(config.get("tracer"))
                        .build();
    }

    @Test
    void testTelemetryWithTracer() {
        Config config = Config.just(ConfigSources.create(
                """
                        telemetry:
                          service: "test-otel"
                          global: false
                          signals:
                            tracing:
                              sampler:
                                type: "always_on"
                              exporters:
                                - type: otlp
                                  protocol: http/proto
                                  name: my-oltp
                                - type: zipkin
                              processors:
                                - max-queue-size: 21
                                  type: batch
                        """,
                MediaTypes.APPLICATION_YAML));

        OpenTelemetry openTelemetry = OpenTelemetry.builder().config(config.get("telemetry")).build();

        assertThat("Helidon OpenTelemetry", openTelemetry, notNullValue());
        assertThat("Propagators",
                   openTelemetry.prototype().propagators(),
                   hasItems(instanceOf(W3CBaggagePropagator.class),
                            instanceOf(W3CTraceContextPropagator.class)));

        assertThat("Service name", openTelemetry.prototype().service(), is("test-otel"));
        assertThat("Enabled", openTelemetry.prototype().enabled(), is(true));
        assertThat("Global", openTelemetry.prototype().global(), is(false));


    }

//    @Disabled
    @Test
    void testUsingServiceRegistry() {

        Telemetry telemetry = Services.get(Telemetry.class);
        assertThat(telemetry, notNullValue());


//        Optional<Telemetry.Signal<Tracer>> tracerSignal = openTelemetry.signal(Tracer.class);
//
//        assertThat("Tracer from OTel tracer signal",
//                   tracerSignal.map(signal -> signal.get("test-tracer")),
//                   OptionalMatcher.optionalValue(instanceOf(Tracer.class)));
    }
}
