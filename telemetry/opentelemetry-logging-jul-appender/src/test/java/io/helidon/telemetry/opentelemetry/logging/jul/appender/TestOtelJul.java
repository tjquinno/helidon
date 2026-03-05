/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.telemetry.opentelemetry.logging.jul.appender;

import io.helidon.service.registry.Services;
import io.helidon.telemetry.otelconfig.OpenTelemetryConfig;
import io.helidon.telemetry.otelconfig.OpenTelemetryLoggingConfig;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

class TestOtelJul {


    @Test
    void testHandlerInit() {

        var inMemoryExporter = InMemoryLogRecordExporter.create();

        var loggingConfig = OpenTelemetryLoggingConfig.builder()
                .addProcessor(SimpleLogRecordProcessor.builder(inMemoryExporter).build())
                .build();

        var openTelemetryConfig = OpenTelemetryConfig.builder()
                .service("test-service")
                .logging(loggingConfig)
                .build();

        Services.set(OpenTelemetry.class, openTelemetryConfig.openTelemetry());

        new OtelJulHandlerProvider(openTelemetryConfig.openTelemetry());

        java.util.logging.Logger.getLogger("test-logger").log(java.util.logging.Level.INFO, "log message 1");

        var records = inMemoryExporter.getFinishedLogRecordItems();

        assertThat("Retrieved records", records, hasSize(greaterThan(0)));

    }
}

