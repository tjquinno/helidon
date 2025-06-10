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

import java.util.List;

import io.helidon.common.config.Config;
import io.helidon.health.spi.HealthCheckProvider;
import io.helidon.service.registry.Service;

/**
 * Test health check provider declared as a Helidon service.
 */
@Service.Singleton
public class ServiceBasedHealthCheckProvider implements HealthCheckProvider {

    static final HealthCheck ALWAYS_UP_SERVICE_BASED_STARTUP = new ServiceBasedAlwaysUpStartupHealthCheck();

    static final List<HealthCheck> ALL_SERVICE_BASED_AUTO_HEALTH_CHECKS = List.of(ALWAYS_UP_SERVICE_BASED_STARTUP);

    @Override
    public List<HealthCheck> healthChecks(Config config) {
        return ALL_SERVICE_BASED_AUTO_HEALTH_CHECKS;
    }

    private static class ServiceBasedAlwaysUpStartupHealthCheck implements HealthCheck {

        static final String ALWAYS_UP_SERVICE_BASED_STARTUP_NAME = "always-up-service-based-startup";

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder()
                    .status(HealthCheckResponse.Status.UP)
                    .detail("service-based", true)
                    .build();
        }

        @Override
        public String name() {
            return ALWAYS_UP_SERVICE_BASED_STARTUP_NAME;
        }

        @Override
        public HealthCheckType type() {
            return HealthCheckType.STARTUP;
        }
    }
}
