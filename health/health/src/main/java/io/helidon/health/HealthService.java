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
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

/**
 * Collects health checks and prepares them according to configuration during initialization, then executes the active
 * checks when {@code checkHealth} is invoked.
 */
@RuntimeType.PrototypedBy(HealthServiceConfig.class)
public interface HealthService extends RuntimeType.Api<HealthServiceConfig> {

    /**
     * Health service logger.
     */
    System.Logger LOGGER = System.getLogger(HealthService.class.getName());

    /**
     * Creates a new builder.
     *
     * @return new builder
     */
    static HealthServiceConfig.Builder builder() {
        return HealthServiceConfig.builder();
    }

    /**
     * Creates a new {@code HealthService} with default values.
     *
     * @return new health service with default values
     */
    static HealthService create() {
        return new HealthServiceImpl();
    }

    /**
     * Creates a new {@code HealthService} using the specified health service configuration settings.
     *
     * @param healthConfig health service configuration
     * @return new health service with the specified values
     */
    static HealthService create(HealthServiceConfig healthConfig) {
        return new HealthServiceImpl(healthConfig);
    }

    /**
     * Creates a new {@code HealthService} by creating a new builder and passing it to the specified builder consumer.
     *
     * @param consumer {@link Consumer} of {@link HealthServiceConfig.Builder} to prepare the builder
     * @return new health service created using the builder modified by the builder consumer
     */
    static HealthService create(Consumer<HealthServiceConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * Reports health check responses for all known and non-excluded health checks.
     *
     * @return {@link HealthCheckResponse} instances for the relevant health checks
     */
    default List<HealthCheckResponse> checkHealth() {
        return checkHealth(Set.of(HealthCheckType.values()));
    }

    /**
     * Reports health check responses for the given health check type.
     *
     * @param healthCheckType health check type of interest
     * @return health check responses for the indicated health check type
     */
    List<HealthCheckResponse> checkHealth(HealthCheckType healthCheckType);

    /**
     * Reports health check responses for all relevant health checks of the indicated check types.
     *
     * @param healthCheckTypes types of health checks to invoke
     * @return {@link HealthCheckResponse} instances for the selected health checks
     */
    List<HealthCheckResponse> checkHealth(Set<HealthCheckType> healthCheckTypes);

    /**
     * Returns the known health checks.
     *
     * @return health checks
     */
    List<HealthCheck> checks();

    /**
     * Reports the overall health status derived by combining the status from invoking all relevant health checks if
     * health is enabled, {@linkplain HealthCheckResponse.Status#UP UP} if health is disabled.
     *
     * @return overall health status using all relevant checks
     */
    default HealthCheckResponse.Status status() {
        return status(checkHealth());
    }

    /**
     * Reports the overall health status derived by combining the status from invoke all relevant health checks with
     * one of the indicated types if health is enabled, {@linkplain HealthCheckResponse.Status#UP UP} if health is
     * disabled.
     *
     * @param healthCheckTypes types of health checks to invoke
     * @return overall health status using relevant checks of the specified types
     */
    default HealthCheckResponse.Status status(Set<HealthCheckType> healthCheckTypes) {
        return status(checkHealth(healthCheckTypes));

    }

    private static HealthCheckResponse.Status status(List<HealthCheckResponse> healthCheckResponses) {
        return healthCheckResponses.stream()
                .map(HealthCheckResponse::status)
                .reduce(HealthCheckResponse.Status::poorer)
                .orElse(HealthCheckResponse.Status.UP);
    }

}
