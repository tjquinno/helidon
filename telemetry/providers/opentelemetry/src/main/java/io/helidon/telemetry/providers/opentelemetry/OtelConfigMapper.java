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

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import io.helidon.common.Weight;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigMapperProvider;
import io.helidon.service.registry.Service;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Config mappers for OpenTelemetry-related types.
 * <p>
 * Span exporters and span processors in particular have different implementations, selected by {@code type} settings in their
 * respective configuration blueprints. When the config system needs to create an exporter or processor based on configuration
 * this mapper uses the configured type indicator to construct the correct concrete implementation.
 */
@Service.Singleton
@Weight(120d)
public class OtelConfigMapper implements ConfigMapperProvider {

    @Override
    public Map<Class<?>, Function<Config, ?>> mappers() {
        return Map.of(SpanExporter.class, OtelConfigMapper::createSpanExporter);
    }

    private static SpanExporter createSpanExporter(Config spanExporterConfig) {
        SpanExporterConfig exporterConfig = SpanExporterConfig.create(spanExporterConfig);

        return switch (exporterConfig.type()) {
            case ZIPKIN -> createZipkinSpanExporter(spanExporterConfig);
            case CONSOLE -> LoggingSpanExporter.create();
            case LOGGING_OTLP -> OtlpJsonLoggingSpanExporter.create();
            case OTLP -> OtlpExporterConfigSupport.createOtlpSpanExporter(spanExporterConfig);
        };
    }

    private static ZipkinSpanExporter createZipkinSpanExporter(Config config) {
        ZipkinSpanExporterBuilder builder = ZipkinSpanExporter.builder();

        var zipkinConfig = ZipkinExporterConfig.create(config);

        zipkinConfig.compression().map(CompressionType::value).ifPresent(builder::setCompression);
        zipkinConfig.endpoint().map(URI::toASCIIString).ifPresent(builder::setEndpoint);
        zipkinConfig.timeout().ifPresent(builder::setReadTimeout);
        zipkinConfig.sender().ifPresent(builder::setSender);
        zipkinConfig.localIpAddressSupplier().ifPresent(builder::setLocalIpAddressSupplier);
        zipkinConfig.meterProvider().ifPresent(builder::setMeterProvider);

        return builder.build();
    }
}

