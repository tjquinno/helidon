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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TestStatus {

    @Test
    void testStatusComparisons() {
        assertThat("Poorer of UP vs. DOWN",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.UP, HealthCheckResponse.Status.DOWN),
                is(HealthCheckResponse.Status.DOWN));
        assertThat("Poorer of DOWN vs. UP",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.DOWN, HealthCheckResponse.Status.UP),
                is(HealthCheckResponse.Status.DOWN));

        assertThat("Poorer of UP vs. ERROR",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.UP, HealthCheckResponse.Status.ERROR),
                is(HealthCheckResponse.Status.ERROR));
        assertThat("Poorer of ERROR vs. UP",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.ERROR, HealthCheckResponse.Status.UP),
                is(HealthCheckResponse.Status.ERROR));

        assertThat("Poorer of DOWN vs. ERROR",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.DOWN, HealthCheckResponse.Status.ERROR),
                is(HealthCheckResponse.Status.ERROR));
        assertThat("Poorer of ERROR vs. DOWN",
                HealthCheckResponse.Status.poorer(HealthCheckResponse.Status.DOWN, HealthCheckResponse.Status.ERROR),
                is(HealthCheckResponse.Status.ERROR));
    }
}
