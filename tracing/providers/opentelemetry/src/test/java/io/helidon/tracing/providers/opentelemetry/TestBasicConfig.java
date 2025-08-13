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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.Services;
import io.helidon.telemetry.providers.opentelemetry.TracerBuilderConfig;
import io.helidon.tracing.Tracer;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

class TestBasicConfig {


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

        OpenTelemetryTracing openTelemetryTracing = OpenTelemetryTracing.create(config.get("telemetry.signals.tracing"));

        assertThat("Helidon OTel tracing", openTelemetryTracing, is(notNullValue()));

        OpenTelemetryTracingConfig prototype = openTelemetryTracing.prototype();

        assertThat("Exporters",
                   prototype.exporters().values(),
                   allOf(hasItems(instanceOf(OtlpHttpSpanExporter.class),
                                  instanceOf(ZipkinSpanExporter.class)),
                         iterableWithSize(2)));

    }

    @Test
    void testUsingServiceRegistry() {

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

        Services.set(Config.class, config);
        Tracer tracer = Services.get(Tracer.class);
        assertThat("Tracer", tracer, allOf(notNullValue(),
                                           instanceOf(OpenTelemetryTracer.class)));

        OpenTelemetryTracer openTelemetryTracer = (OpenTelemetryTracer) tracer;
        openTelemetryTracer.unwrap(io.opentelemetry.api.trace.Tracer.class);


    }

    @Test
    void testLegacyTracingConfig() {
        String legacyText = """
                tracing:
                  service: "test-otel"
                  propagation: ["b3"]
                  collector-port: 1234
                  collector-path: "/record"
                """;
        Config config = Config.just(ConfigSources.create(legacyText, MediaTypes.APPLICATION_YAML));
        TracerBuilderConfig c = TracerBuilderConfig.create(config.get("tracing"));
        io.helidon.common.config.Config result = c.asConfig();


    }
}
