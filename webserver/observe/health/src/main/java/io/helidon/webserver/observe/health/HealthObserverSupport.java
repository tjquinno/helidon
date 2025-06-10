/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.function.Supplier;

import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;
import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.health.HealthServiceConfig;
import io.helidon.service.registry.Services;

final class HealthObserverSupport {
    private HealthObserverSupport() {
    }

    private static List<Config> configFragments(Config candidateParentOfConfigFragments) {
        return candidateParentOfConfigFragments.asNodeList().orElse(List.of());
    }

    static final class CustomMethods {
        private CustomMethods() {
        }

        /**
         * Add the provided health check using an explicit type (may differ from the
         * {@link io.helidon.health.HealthCheck#type()}.
         *
         * @param builder required for the custom method
         * @param check   health check to add
         * @param type    type to use
         * @deprecated Use {@link io.helidon.webserver.observe.health.HealthService} and related types instead.
         */
        @Deprecated(since = "4.2.4", forRemoval = true)
        @Prototype.BuilderMethod
        static void addCheck(HealthObserverConfig.BuilderBase<?, ?> builder, HealthCheck check, HealthCheckType type) {
            if (check.type() == type) {
                builder.addCheck(check);
            } else {
                builder.addCheck(new TypedCheck(check, type));
            }
        }

        /**
         * Add a health check using the provided response supplier, type, and name.
         *
         * @param builder          required for the custom method
         * @param responseSupplier supplier of the health check response
         * @param type             type to use
         * @param name             name to use for the health check
         */
        @Prototype.BuilderMethod
        static void addCheck(HealthObserverConfig.BuilderBase<?, ?> builder,
                             Supplier<HealthCheckResponse> responseSupplier,
                             HealthCheckType type,
                             String name) {
            addCheck(builder, new HealthCheck() {
                         @Override
                         public HealthCheckResponse call() {
                             return responseSupplier.get();
                         }

                         @Override
                         public String name() {
                             return name;
                         }
                     },
                     type);
        }

        /**
         * Add the provided health checks.
         *
         * @param builder required for the custom method
         * @param checks  health checks to add
         */
        @Prototype.BuilderMethod
        static void addChecks(HealthObserverConfig.BuilderBase<?, ?> builder, HealthCheck[] checks) {
            for (HealthCheck healthCheck : checks) {
                builder.addCheck(healthCheck);
            }
        }
    }

    /**
     * Builder decorator.
     * <p>
     * For backwards compatibility, the observer config temporarily continues to support adding checks and containing config
     * fragments to tailor the settings for individual health checks, and therefore the observer creates and registers the
     * {@link io.helidon.webserver.observe.health.HealthService}.
     */
    @Deprecated(since = "4.2.4", forRemoval = true)
    static class BuilderDecorator implements Prototype.BuilderDecorator<HealthObserverConfig.BuilderBase<?, ?>> {

        @Override
        public void decorate(HealthObserverConfig.BuilderBase<?, ?> target) {

            HealthServiceConfig.Builder healthServiceConfigBuilder = HealthServiceConfig.builder()
                    .config(Services.get(Config.class).get("health"))
                    .checks(target.healthService().checks());
            target.config().ifPresent(c -> {
                healthServiceConfigBuilder.checksConfig(configFragments(c))
                        .config(c);
            });
            Services.set(io.helidon.health.HealthService.class, healthServiceConfigBuilder.build());
        }
    }

    private static final class TypedCheck implements HealthCheck {
        private final HealthCheck delegate;
        private final HealthCheckType type;

        private TypedCheck(HealthCheck delegate, HealthCheckType type) {
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public HealthCheckType type() {
            return type;
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public String path() {
            return delegate.path();
        }

        @Override
        public HealthCheckResponse call() {
            return delegate.call();
        }
    }
}
