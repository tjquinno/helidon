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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;
import io.helidon.config.ConfigSources;

class TracerBuilderConfigSupport {

    private TracerBuilderConfigSupport() {
    }

    private static String tl(String suffix) {
        return "telemetry." + suffix;
    }

    private static String tr(String suffix) {
        return "telemetry.signals.tracing." + suffix;
    }


    static class CustomMethods extends OpenTelemetryTracingConfigSupport.CustomMethods {

        @Prototype.PrototypeMethod
        static Config asConfig(TracerBuilderConfig cfg) {
            var x = Transformer.create();

            // First, the telemetry-related settings.
            x.tl("service", cfg.serviceName())
                    .tl("global", cfg.global())
                    .tl("propagators", cfg.propagation())

                    // Mow the tracer-related settings.
                    .tr("protocol", cfg.collectorProtocol())
                    .tr("collector.port", cfg.collectorPort())
                    .tr("collector.path", cfg.collectorPath())
                    .tr("collector.host", cfg.collectorHost())
                    .tr("client-cert-pem", cfg.clientCertPem())
                    .tr("exporter.timeout", cfg.exporterTimeout())
                    .tr("max-export-batch-size", cfg.maxExportBatchSize())
                    .tr("max-queue-size", cfg.maxQueueSize())
                    .tr("private-key-pem", cfg.privateKeyPem())
                    .tr("sampler.param", cfg.samplerParam())
                    .tr("sampler.type", cfg.samplerType())
                    .tr("schedule-delay", cfg.scheduleDelay())
                    .tr("processors.0.type", cfg.spanProcessorType())
                    .tr("trusted-cert-pem", cfg.trustedCertPem());

            return x.config();
        }

    }

    private record Transformer(Map<String, String> map) {

        static Transformer create() {
            return new Transformer(new LinkedHashMap<>() { });
        }

        Transformer tl(String key, String value) {
            map.put("telemetry." + key, value);
            return this;
        }

        Transformer tl(String key, Optional<String> value) {
            value.ifPresent(v -> tl(key, v));
            return this;
        }

        Transformer tr(String key, Optional<String> value) {
            value.ifPresent(v -> map.put("telemetry.tracing." + key, v));
            return this;
        }

        Config config() {
            return io.helidon.config.Config.just(ConfigSources.create(map));
        }
    }
}
