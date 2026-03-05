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

import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

class OtelJulHandler extends Handler {

    private static final Map<Level, Severity> LEVELS = Map.of(
            Level.SEVERE, Severity.ERROR,
            Level.WARNING, Severity.WARN,
            Level.INFO, Severity.INFO,
            Level.CONFIG, Severity.INFO,
            Level.FINE, Severity.DEBUG,
            Level.FINER, Severity.DEBUG,
            Level.FINEST, Severity.TRACE);

    private final Logger otelLogger;

    OtelJulHandler(OpenTelemetry openTelemetry) {
        otelLogger = openTelemetry
                .getLogsBridge()
                .loggerBuilder("jul")
                .build();
    }

    static OtelJulHandler create(OpenTelemetry openTelemetry) {
        return new OtelJulHandler(openTelemetry);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        LogRecordBuilder builder = otelLogger.logRecordBuilder();
        builder.setBody(record.getMessage())
                .setSeverity(LEVELS.get(record.getLevel()))
                .setTimestamp(record.getMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

        builder.setAttribute("logger.name", record.getLoggerName());
        builder.setAttribute("thread.id", record.getLongThreadID());
        if (record.getThrown() != null) {
            builder.setAttribute("exception.message", record.getThrown().getMessage());
            builder.setAttribute("exception", record.getThrown().toString());
        }
        builder.emit();
    }

    @Override
    public void flush() {
        // Nothing to do here with OpenTelemetry.
    }

    @Override
    public void close() {
        // Nothing to do here with OpenTelemetry.
    }
}
