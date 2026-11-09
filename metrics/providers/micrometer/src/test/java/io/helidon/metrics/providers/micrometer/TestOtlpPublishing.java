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
package io.helidon.metrics.providers.micrometer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class TestOtlpPublishing {

    @SuppressWarnings("removal")
    static Stream<Arguments> publisherConfigurations() {
        return Stream.of(
                Arguments.of(Named.of("default", OtlpPublisher.builder().config(Config.empty())), false),
                Arguments.of(Named.of("configured false", OtlpPublisher.builder()
                        .config(Config.just("publish-max-gauge-for-histograms: false", MediaTypes.APPLICATION_YAML))), false),
                Arguments.of(Named.of("configured true", OtlpPublisher.builder()
                        .config(Config.just("publish-max-gauge-for-histograms: true", MediaTypes.APPLICATION_YAML))), true),
                Arguments.of(Named.of("programmatic false", OtlpPublisher.builder()
                        .config(Config.empty()).publishMaxGaugeForHistograms(false)), false),
                Arguments.of(Named.of("programmatic true", OtlpPublisher.builder()
                        .config(Config.empty()).publishMaxGaugeForHistograms(true)), true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("publisherConfigurations")
    void testHistogramMetricNames(OtlpPublisherConfig.Builder publisherConfig, boolean publishMaxGauges) throws Exception {
        CompletableFuture<byte[]> received = new CompletableFuture<>();
        HttpServer receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        receiver.createContext("/metrics", exchange -> {
            try (exchange) {
                received.complete(exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(200, -1);
            }
        });
        receiver.start();
        try {
            var registry = publisherConfig
                    .url("http://127.0.0.1:" + receiver.getAddress().getPort() + "/metrics")
                    .build()
                    .registry()
                    .get();
            try {
                registry.timer("compat.latency").record(Duration.ofMillis(250));
                registry.summary("compat.payload").record(42);
                registry.gauge("compat.intentional.max", 7);
            } finally {
                // Closing flushes the recorded metrics without waiting for the scheduled export.
                registry.close();
            }

            // Protobuf string fields retain these ASCII names verbatim in the exported payload.
            String payload = new String(received.get(5, TimeUnit.SECONDS), StandardCharsets.ISO_8859_1);
            assertThat("Explicitly registered metric names", payload,
                       allOf(containsString("compat.latency"),
                             containsString("compat.payload"),
                             containsString("compat.intentional.max")));
            assertThat("Timer maximum gauge", payload,
                       publishMaxGauges ? containsString("compat.latency.max") : not(containsString("compat.latency.max")));
            assertThat("Summary maximum gauge", payload,
                       publishMaxGauges ? containsString("compat.payload.max") : not(containsString("compat.payload.max")));
        } finally {
            receiver.stop(0);
        }
    }
}
