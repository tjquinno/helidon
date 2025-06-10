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
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.common.config.Config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;

class TestService {

    static final HealthCheck ALWAYS_DOWN_LIVENESS = new AlwaysDownLiveness();
    static final HealthCheck ALWAYS_UP_READINESS = new AlwaysUpReadiness();
    static final HealthCheck ALWAYS_UP_STARTUP = new AlwaysUpStartup();

    private static final List<HealthCheck> ALL_EXPLICIT_CHECKS = List.of(ALWAYS_DOWN_LIVENESS,
                                                                         ALWAYS_UP_READINESS,
                                                                         ALWAYS_UP_STARTUP);


    @Test
    void testDisabled() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .enabled(false)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .build();

        assertThat("Relevant checks", healthService.checks(), emptyIterable());

        assertThat("Overall status",
                   healthService.status(),
                   is(HealthCheckResponse.Status.UP));
    }

    @Test
    void testWithUseSystemServices() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(true)
                .build();

        List<HealthCheck> expectedHealthChecks = new ArrayList<>(TestHealthCheckProvider.allAutoHealthChecks);
        expectedHealthChecks.addAll(ServiceBasedHealthCheckProvider.ALL_SERVICE_BASED_AUTO_HEALTH_CHECKS);

        assertThat("Relevant checks",
                   healthService.checks(),
                   containsInAnyOrder(expectedHealthChecks.toArray(new HealthCheck[0])));

        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.DOWN));
    }

    @Test
    void testIncludedByName() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(false)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addInclude(ALWAYS_UP_READINESS.name())
                .build();

        assertThat("Relevant checks", healthService.checks(), contains(ALWAYS_UP_READINESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.UP));

        healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(true)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addInclude(ALWAYS_DOWN_LIVENESS.name())
                .build();

        assertThat("Relevant checks", healthService.checks(), contains(ALWAYS_DOWN_LIVENESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.DOWN));

        healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(true)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addInclude(TestHealthCheckProvider.AUTO_ALWAYS_DOWN_LIVENESS.name())
                .build();

        assertThat("Relevant checks",
                   healthService.checks(),
                   contains(TestHealthCheckProvider.AUTO_ALWAYS_DOWN_LIVENESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.DOWN));
    }

    @Test
    void testIncludedByClass() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(false)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addIncludeClass(AlwaysUpReadiness.class)
                .build();

        assertThat("Relevant checks", healthService.checks(), contains(ALWAYS_UP_READINESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void testExcludedByName() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(false)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addExclude(ALWAYS_DOWN_LIVENESS.name())
                .build();

        assertThat("Relevant checks", healthService.checks(), contains(ALWAYS_UP_READINESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void testExcludedByClass() {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(false)
                .addCheck(ALWAYS_DOWN_LIVENESS)
                .addCheck(ALWAYS_UP_READINESS)
                .addExcludeClass(AlwaysDownLiveness.class)
                .build();

        assertThat("Relevant checks", healthService.checks(), contains(ALWAYS_UP_READINESS));
        assertThat("Overall status", healthService.status(), is(HealthCheckResponse.Status.UP));
    }

    @ParameterizedTest
    @MethodSource
    void testWithSelectedTypes(boolean useSystemServices,
                               List<HealthCheck> checksToAddExplicitly,
                               Set<HealthCheckType> typesToCheck,
                               Set<HealthCheck> expectedChecks,
                               HealthCheckResponse.Status expectedStatus) {
        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(useSystemServices)
                .checks(checksToAddExplicitly)
                .build();

        assertThat("Relevant checks",
                   healthService.checks(typesToCheck),
                   containsInAnyOrder(expectedChecks.toArray(new HealthCheck[0])));
        assertThat("Overall status", healthService.status(typesToCheck), is(expectedStatus));
    }

    @Test
    void testWithSelectedTypesWithDiscoveredChecks() {

        var healthService = (HealthServiceImpl) HealthServiceConfig.builder()
                .useSystemServices(true)
                .checks(ALL_EXPLICIT_CHECKS)
                .build();

        assertThat("Relevant checks",
                   healthService.checks(Set.of(HealthCheckType.READINESS)),
                   containsInAnyOrder(ALWAYS_UP_READINESS, TestHealthCheckProvider.autoAlwaysUpReadiness));
        assertThat("Overall status",
                   healthService.status(Set.of(HealthCheckType.READINESS)),
                   is(HealthCheckResponse.Status.UP));
    }

    static Stream<Arguments> testWithSelectedTypes() {

        // Any time the liveness check is included as a health check and liveness is one of the check types used for filtering,
        // the resulting status should be DOWN. Otherwise, expect UP.

        return Stream.of(Arguments.arguments(
                                 false,
                                 ALL_EXPLICIT_CHECKS,
                                 Set.of(HealthCheckType.READINESS,
                                        HealthCheckType.LIVENESS,
                                        HealthCheckType.STARTUP),
                                 Set.of(ALWAYS_UP_READINESS,
                                        ALWAYS_DOWN_LIVENESS,
                                        ALWAYS_UP_STARTUP),
                                 HealthCheckResponse.Status.DOWN),
                         Arguments.arguments(
                                 false,
                                 ALL_EXPLICIT_CHECKS,
                                 Set.of(HealthCheckType.READINESS,
                                        HealthCheckType.STARTUP),
                                 Set.of(ALWAYS_UP_READINESS,
                                        ALWAYS_UP_STARTUP),
                                 HealthCheckResponse.Status.UP),
                         Arguments.arguments(
                                 false,
                                 ALL_EXPLICIT_CHECKS,
                                 Set.of(HealthCheckType.READINESS,
                                        HealthCheckType.LIVENESS),
                                 Set.of(ALWAYS_UP_READINESS,
                                        ALWAYS_DOWN_LIVENESS),
                                 HealthCheckResponse.Status.DOWN),
                         Arguments.arguments(
                                 false,
                                 ALL_EXPLICIT_CHECKS,
                                 Set.of(HealthCheckType.LIVENESS),
                                 Set.of(ALWAYS_DOWN_LIVENESS),
                                 HealthCheckResponse.Status.DOWN),
                         Arguments.arguments(
                                 false,
                                 ALL_EXPLICIT_CHECKS,
                                 Set.of(HealthCheckType.STARTUP),
                                 Set.of(ALWAYS_UP_STARTUP),
                                 HealthCheckResponse.Status.UP)
                         );

    }

    static class AlwaysDownLiveness implements HealthCheck {

        static final String NAME = "always-down-liveness";

        private final String name;

        AlwaysDownLiveness(String name) {
            this.name = name;
        }

        AlwaysDownLiveness() {
            this(NAME);
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder().status(HealthCheckResponse.Status.DOWN).build();
        }

        @Override
        public String name() {
            return name;
        }
    }

    static class AlwaysUpReadiness implements HealthCheck {

        static final String NAME = "always-up-readiness";

        private final String name;
        private int threshold = 50;

        AlwaysUpReadiness(String name) {
            this.name = name;
        }

        AlwaysUpReadiness() {
            this(NAME);
        }

        AlwaysUpReadiness(String name, Config alwaysUpReadinessConfigNode) {
            this(name);
            alwaysUpReadinessConfigNode.get("threshold").asInt().ifPresent(threshold -> this.threshold = threshold);
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder().status(HealthCheckResponse.Status.UP).build();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HealthCheckType type() {
            return HealthCheckType.READINESS;
        }

        int threshold() {
            return threshold;
        }
    }

    static class AlwaysUpStartup implements HealthCheck {

        static final String NAME = "always-up-startup";

        private final String name;

        private int threshold = 50;

        AlwaysUpStartup(String name) {
            this.name = name;
        }

        AlwaysUpStartup() {
            this(NAME);
        }

        AlwaysUpStartup(String name, Config alwaysUpStartupConfigNode) {
            this(name);
            alwaysUpStartupConfigNode.get("threshold").asInt().ifPresent(threshold -> this.threshold = threshold);
        }

        @Override
        public HealthCheckType type() {
            return HealthCheckType.STARTUP;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder().status(HealthCheckResponse.Status.UP).build();
        }

        int threshold() {
            return threshold;
        }
    }
}
