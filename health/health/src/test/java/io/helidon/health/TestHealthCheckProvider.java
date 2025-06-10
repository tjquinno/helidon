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
import java.util.stream.Stream;

import io.helidon.common.config.Config;
import io.helidon.health.spi.HealthCheckProvider;

public class TestHealthCheckProvider implements HealthCheckProvider {

    static final String AUTO_ALWAYS_DOWN_LIVENESS_NAME = "auto-always-down-liveness";
    static final String AUTO_ALWAYS_UP_READINESS_NAME = "auto-always-up-readiness";
    static final String AUTO_ALWAYS_UP_STARTUP_NAME = "auto-always-up-startup";

    static final HealthCheck AUTO_ALWAYS_DOWN_LIVENESS = new TestService.AlwaysDownLiveness(AUTO_ALWAYS_DOWN_LIVENESS_NAME);
    static final HealthCheck AUTO_ALWAYS_UP_STARTUP = new TestService.AlwaysUpStartup(AUTO_ALWAYS_UP_STARTUP_NAME);

    static HealthCheck autoAlwaysUpReadiness;
    static HealthCheck autoAlwaysUpStartup;

    static List<HealthCheck> allAutoHealthChecks = new ArrayList<>();

    @Override
    public List<HealthCheck> healthChecks(Config config) {
        allAutoHealthChecks.clear();
        // The following mimics what the Helidon built-in health check provider does: pass the config for each check to
        // the factory for that check.
        autoAlwaysUpReadiness = new TestService.AlwaysUpReadiness(AUTO_ALWAYS_UP_READINESS_NAME,
                                                                  config.get(AUTO_ALWAYS_UP_READINESS_NAME));
        autoAlwaysUpStartup = new TestService.AlwaysUpStartup(AUTO_ALWAYS_UP_STARTUP_NAME,
                                                              config.get(AUTO_ALWAYS_UP_STARTUP_NAME));
        Stream.of(AUTO_ALWAYS_DOWN_LIVENESS,
                  autoAlwaysUpStartup,
                  autoAlwaysUpReadiness
                )
                .forEach(healthCheck -> allAutoHealthChecks.add(healthCheck));

        return allAutoHealthChecks;
    }
}
