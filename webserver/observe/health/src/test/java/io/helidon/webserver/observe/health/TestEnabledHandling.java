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

package io.helidon.webserver.observe.health;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.common.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.checks.DiskSpaceHealthCheck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

class TestEnabledHandling {

    private static final HealthCheck CHECK_1 = new HealthCheck() {

        @Override
        public String name() {
            return "check-1";
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder().build();
        }
    };

    private static final HealthCheck CHECK_2 = new HealthCheck() {

        @Override
        public String name() {
            return "check-2";
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder().build();
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"helidon.health", "checks"})
    // @Deprecated(since = "4.2.4", forRemoval = true) If/when we remove support for helidon.health as a container of
    // check config then we won't need to work with that config prefix here.
    void testDisabledViaConfig(String configPrefix) {
        List<HealthCheck> originalChecks = List.of(CHECK_1, CHECK_2);
        Map<String, String> configSettings = Map.of(configPrefix + ".check-1.enabled", "false");
        Config config = io.helidon.config.Config.just(ConfigSources.create(configSettings));

        List<HealthCheck> enabledChecks = HealthObserver.enabledHealthChecks(config, originalChecks);

        assertThat("Accumulated checks", enabledChecks, contains(CHECK_2));

        configSettings = Map.of(configPrefix + ".check-1.enabled", "true");
        config = io.helidon.config.Config.just(ConfigSources.create(configSettings));

        enabledChecks = HealthObserver.enabledHealthChecks(config, originalChecks);

        assertThat("Accumulated checks", enabledChecks, containsInAnyOrder(CHECK_1, CHECK_2));

    }

    @ParameterizedTest
    @ValueSource(strings = {"helidon.health", "checks"})
    void testBuiltInChecks(String configPrefix) {
        // Use the observer which loads checks via providers--including the built-in health checks.
        Map<String, String> configSettings = Map.of(configPrefix + ".check-1.enabled", "false",
                                                    configPrefix + ".heapMemory.enabled", "false");
        Config config = io.helidon.config.Config.just(ConfigSources.create(configSettings));

        HealthObserver healthObserver = HealthObserverConfig.builder()
                .config(config)
                .addCheck(CHECK_1)
                .addCheck(CHECK_2)
                .build();

        Map<String, HealthCheck> checksByName = healthObserver.all().stream()
                .collect(HashMap::new,
                         (map, check) -> map.put(check.name(), check),
                         HashMap::putAll);

        assertThat("Checks from observer",
                   checksByName,
                   allOf(hasKey(CHECK_2.name()),
                         hasKey("diskSpace"),
                         not(hasKey(CHECK_1.name())),
                         not(hasKey("heapMemory"))));

    }

}
