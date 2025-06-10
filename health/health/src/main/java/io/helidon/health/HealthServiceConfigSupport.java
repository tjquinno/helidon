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
import java.util.function.Supplier;

import io.helidon.builder.api.Prototype;
import io.helidon.common.Errors;
import io.helidon.common.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.health.spi.HealthCheckProvider;
import io.helidon.service.registry.Services;

class HealthServiceConfigSupport {

    private HealthServiceConfigSupport() {
    }

    static class BuilderDecorator implements Prototype.BuilderDecorator<HealthServiceConfig.BuilderBase<?, ?>> {

        @Override
        public void decorate(HealthServiceConfig.BuilderBase<?, ?> target) {

            var collector = Errors.collector();

            checkClassesForFiltering(collector, "include", target.includeClasses());
            checkClassesForFiltering(collector, "exclude", target.excludeClasses());

            // Warn the user if any include class or exclude class is not assignable to HealthCheck.

            // Replace the current contents of the healthChecks list with health checks from
            // providers (if they are to be used) plus its original value (set by application code), all filtered by any
            // included and excluded settings.

            if (target.enabled()) {
                List<HealthCheck> candidateHealthChecks = new ArrayList<>();
                Config checksParentNode = target.config().map(healthConfig -> healthConfig.get("checks")).orElse(Config.empty());

                if (target.useSystemServices()) {
                    Services.all(HealthCheckProvider.class)
                            .forEach(provider ->
                                             candidateHealthChecks.addAll(provider.healthChecks(checksConfig(
                                                     checksParentNode))));
                }

                candidateHealthChecks.addAll(target.checks());

                checkNamesForFiltering(collector, "include", target.include(), candidateHealthChecks);
                checkNamesForFiltering(collector, "exclude", target.exclude(), candidateHealthChecks);

                target.checks(candidateHealthChecks.stream()
                                      .filter(check -> target.include().isEmpty()
                                              || target.include().contains(check.name()))
                                      .filter(check -> target.includeClasses().isEmpty()
                                              || isAssignableFromOne(check, target.includeClasses()))
                                      .filter(check -> target.exclude().isEmpty()
                                              || !target.exclude().contains(check.name()))
                                      .filter(check -> target.excludeClasses().isEmpty()
                                              || !isAssignableFromOne(check, target.excludeClasses()))
                                      .toList());

                collector.collect().log(HealthService.LOGGER);
            } else {
                target.checks(List.of());
            }
        }

        private static void checkClassesForFiltering(Errors.Collector collector,
                                                     String filterType,
                                                     List<Class> classes) {
            var unusableClasses = classes.stream()
                    .filter(c -> !HealthCheck.class.isAssignableFrom(c))
                    .toList();
            if (!unusableClasses.isEmpty()) {
                collector.warn(String.format("The following classes configured for '%s' are not of type HealthCheck: %s",
                                             filterType,
                                             unusableClasses));
            }
        }

        private static void checkNamesForFiltering(Errors.Collector collector,
                                                   String filterType,
                                                   List<String> names,
                                                   List<HealthCheck> candidateHealthChecks) {
            var unmatchedNames = names.stream()
                    .filter(name -> candidateHealthChecks.stream().noneMatch(healthCheck -> healthCheck.name().equals(name)))
                    .toList();
            if (!unmatchedNames.isEmpty()) {
                collector.hint(String.format(
                        "The following names configured for '%s' do not match any discovered health check: %s",
                        filterType,
                        unmatchedNames));
            }
        }

        @SuppressWarnings("unchecked")
        private static boolean isAssignableFromOne(HealthCheck check, List<Class> types) {
            return types.stream()
                    .anyMatch(type -> type.isAssignableFrom(check.getClass()));
        }

        /**
         * Combines the {@code checks} configuration nodes from the top-level health section with the checks in
         * server.features.observe.observers.health.checks for backward compatibility.
         *
         * @param topLevelChecksConfig checks config node from the top-level health config
         * @return config node exposing top-level and health observer check config nodes
         * @deprecated Once the health observer no longer supports checks under its config, remove this method; the caller should
         * just use the checks node under the top-level health config.
         */
        @Deprecated(since = "4.2.4", forRemoval = true)
        private static Config checksConfig(Config topLevelChecksConfig) {
            Config observerChecksConfig = topLevelChecksConfig.root()
                    .get("server.features.observe.observer.health.helidon.health.checks");
            if (!observerChecksConfig.exists()) {
                return topLevelChecksConfig;
            }
            return io.helidon.config.Config.just(
                    () -> ConfigSources.create((io.helidon.config.Config) topLevelChecksConfig),
                    () -> ConfigSources.create((io.helidon.config.Config) observerChecksConfig));
        }
    }

    static class CustomMethods {
        private CustomMethods() {
        }

        /**
         * Add the provided health check using an explicit type (may differ from the
         * {@link io.helidon.health.HealthCheck#type()}.
         *
         * @param builder required for the custom method
         * @param check   health check to add
         * @param type    type to use
         */
        @Prototype.BuilderMethod
        static void addCheck(HealthServiceConfig.BuilderBase<?, ?> builder, HealthCheck check, HealthCheckType type) {
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
        static void addCheck(HealthServiceConfig.BuilderBase<?, ?> builder,
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
        static void addChecks(HealthServiceConfig.BuilderBase<?, ?> builder, HealthCheck[] checks) {
            for (HealthCheck healthCheck : checks) {
                builder.addCheck(healthCheck);
            }
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
