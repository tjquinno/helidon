/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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

package io.helidon.health.spi;

import java.util.List;

import io.helidon.config.Config;
import io.helidon.health.HealthCheck;

/**
 * {@link java.util.ServiceLoader} provider interface for health check services.
 */
public interface HealthCheckProvider {
    /**
     * Health checks provided by this provider.
     *
     * @param config configuration instance located on root node of this application
     * @return list of health checks
     * @deprecated Use {@link #healthChecks(io.helidon.config.Config)}.
     */
    @Deprecated(since = "4.4.0", forRemoval = true)
    default List<HealthCheck> healthChecks(io.helidon.common.config.Config config) {
        throw new UnsupportedOperationException();
    }

    /**
     * Health checks provided by this provider.
     *
     * @param config configuration instance located on root node of this application
     * @return list of health checks
     */
//    default List<HealthCheck> healthChecks(Config config) {
//        return healthChecks((io.helidon.common.config.Config) config);
//    }
    default List<HealthCheck> healthChecks(Config config) {
        if (config instanceof io.helidon.common.config.Config c) {
            return healthChecks(c);
        }
        throw new UnsupportedOperationException();
    }
}
