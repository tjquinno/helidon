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

package io.helidon.health;

import java.util.ArrayList;
import java.util.List;

import io.helidon.common.config.Config;
import io.helidon.service.registry.Services;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ServerTest
class TestConfig {

    private static final Config CONFIG = Services.get(Config.class);

    @Test
    void checkProgrammaticConfig() {
        Config testHealthConfig = CONFIG.get("test").get("no-system-services");

        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(false)
                .addCheck(TestService.ALWAYS_UP_READINESS)
                .addCheck(TestService.ALWAYS_DOWN_LIVENESS)
                .config(testHealthConfig)
                .build();

        assertThat("Relevant checks",
                   healthService.checks(),
                   not(hasItem(TestHealthCheckProvider.autoAlwaysUpReadiness)));

        assertThat("Overall status",
                   healthService.status(),
                   is(HealthCheckResponse.Status.DOWN));

    }

    @Test
    void checkSystemConfig() {
        // The HealthService should gather health checks from HealthCheckProviders via service loading and via
        // Helidon service registration. This checks both test providers, one as a Helidon service and one service loaded.
        var healthService = (HealthServiceImpl) Services.get(HealthService.class);

        List<HealthCheck> expectedHealthChecks = new ArrayList<>(TestHealthCheckProvider.allAutoHealthChecks);
        expectedHealthChecks.addAll(ServiceBasedHealthCheckProvider.ALL_SERVICE_BASED_AUTO_HEALTH_CHECKS);

        assertThat("Relevant checks",
                   healthService.checks(),
                   containsInAnyOrder(expectedHealthChecks.toArray()));

        assertThat("Overall status",
                   healthService.status(),
                   is(HealthCheckResponse.Status.DOWN));

        assertThat("Health check adjusted by config",
                   healthService.checks().stream()
                           .filter(check -> check.name().equals(TestHealthCheckProvider.AUTO_ALWAYS_UP_READINESS_NAME))
                           .map(TestService.AlwaysUpReadiness.class::cast)
                           .map(TestService.AlwaysUpReadiness::threshold)
                           .findFirst()
                           .orElseThrow(),
                   is(75));
    }
}
