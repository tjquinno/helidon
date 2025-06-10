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
import java.util.Map;
import java.util.Set;

import io.helidon.common.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

@Service.Singleton
class HealthServiceImpl implements HealthService {

    private final HealthServiceConfig healthServiceConfig;

    @Service.Inject
    HealthServiceImpl() {
        this(HealthServiceConfig.create(Services.get(Config.class).get("health")));
    }

    HealthServiceImpl(HealthServiceConfig healthConfig) {
        this.healthServiceConfig = healthConfig;
    }

    @Override
    public List<HealthCheckResponse> checkHealth() {
        return healthServiceConfig.checks().stream()
                .map(HealthServiceImpl::call)
                .toList();
    }

    @Override
    public List<HealthCheckResponse> checkHealth(HealthCheckType healthCheckType) {
        return healthServiceConfig.checks().stream()
                .filter(check -> check.type() == healthCheckType)
                .map(HealthCheck::call)
                .toList();
    }

    @Override
    public List<HealthCheckResponse> checkHealth(Set<HealthCheckType> healthCheckTypes) {
        return healthServiceConfig.checks().stream()
                .filter(check -> healthCheckTypes.contains(check.type()))
                .map(HealthCheck::call)
                .toList();
    }

    @Override
    public HealthServiceConfig prototype() {
        return healthServiceConfig;
    }

    @Override
    public List<HealthCheck> checks() {
        return healthServiceConfig.checks();
    }

    List<HealthCheck> checks(Set<HealthCheckType> healthCheckTypes) {
        return healthServiceConfig.checks().stream()
                .filter(check -> healthCheckTypes.contains(check.type()))
                .toList();
    }

    private static HealthCheckResponse call(HealthCheck check) {
        var rawResponse = check.call();
        try {
            return (rawResponse.name() != null && !rawResponse.name().isEmpty())
                    ? rawResponse
                    : new NamedHealthCheckResponse(check.name(), rawResponse);
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                    .name(check.name())
                    .status(HealthCheckResponse.Status.ERROR)
                    .detail("error", e.getClass().getName())
                    .detail("message", e.getMessage())
                    .build();
        }
    }

    /**
     * Wrapper around a response with no name itself to associate a name with it.
     *
     * @param name name to use with the response
     * @param delegate underlying nameless response
     */
    private record NamedHealthCheckResponse(String name, HealthCheckResponse delegate) implements HealthCheckResponse {

        @Override
        public Status status() {
            return delegate.status();
        }

        @Override
        public Map<String, Object> details() {
            return delegate.details();
        }
    }
}
