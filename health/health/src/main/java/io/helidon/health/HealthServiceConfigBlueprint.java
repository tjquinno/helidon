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

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

/**
 * Health configuration (excluding health endpoint settings).
 * <p>
 * The health subsystem configuration gathers candidate health checks from these sources:
 * <ul>
 *     <li>implementations of {@link io.helidon.health.spi.HealthCheckProvider} declared as Helidon services,</li>
 *     <li>implementations of {@link io.helidon.health.spi.HealthCheckProvider} via service loading (legacy), and</li>
 *     <li>health checks added explicitly to the configuration object by the application.</li>
 * </ul>
 * <p>
 * From those, Helidon chooses the health checks to use as follows:
 * <ol>
 *     <li>If health is disabled, ignore all checks. (Reported status will always be {@code UP}).</li>
 *     <li>If {@code use-system-services} is true, gather health checks using {@link io.helidon.health.spi.HealthCheckProvider}
 *     instances.</li>
 *     <li>Add all health checks explicitly added to the builder using its
 *     {@linkplain HealthServiceConfig.Builder#addCheck addHealthCheck}
 *     and {@linkplain HealthServiceConfig.Builder#checks(List) addHealthChecks} methods.</li>
 *     <li>If the configuration settings explicitly include any health checks, either by name using {@code include} or
 *     by type using {@code include-classes}, keep only the checks assembled so far that <em>are also</em> explicitly
 *     included.</li>
 *     <li>If the configuration settings explicitly exclude any health checks, either by name using {@code exclude} or
 *     by type using {@code exclude-classes}, keep only the checks assembled so far that <em>are not</em> explicitly
 *     excluded.</li>
 * </ol>
 *
 */
@Prototype.Blueprint(decorator = HealthServiceConfigSupport.BuilderDecorator.class)
@Prototype.CustomMethods(HealthServiceConfigSupport.CustomMethods.class)
@Prototype.Configured("health")
@Prototype.RegistrySupport
interface HealthServiceConfigBlueprint extends Prototype.Factory<HealthService> {

    /**
     * If health is enabled.
     * <p>
     * Disabling health also disables the health web endpoint (if it is included on the path).
     *
     * @return true if health is enabled; false otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Names of discovered health checks to include.
     *
     * @return names to include
     */
    @Option.Singular
    @Option.Configured
    List<String> include();

    /**
     * Classes of discovered health checks to include.
     *
     * @return health check classes to include
     */
    @Option.Singular("includeClass")
    @Option.Configured
    List<Class> includeClasses();

    /**
     * Names of discovered health checks to exclude.
     *
     * @return names to exclude
     */
    @Option.Singular
    @Option.Configured
    List<String> exclude();

    /**
     * Classes of discovered health checks to exclude.
     *
     * @return health check classes to exclude
     */
    @Option.Singular("excludeClass")
    @Option.Configured
    List<Class> excludeClasses();

    /**
     * Relevant health checks.
     *
     * @return health checks
     */
    @Option.Singular("check")
    List<HealthCheck> checks();

    /**
     * Configuration to augment settings of named health checks discovered or added explicitly.
     *
     * @return parent config node to named check settings
     * @deprecated For internal Helidon use to support backward compatibility. Visibility might change to package visible in a
     * future major release.
     */
    @Deprecated(since = "4.2.4", forRemoval = true)
    @Option.Configured
    List<Config> checksConfig();

    /**
     * Whether to use health checks furnished by {@link io.helidon.health.spi.HealthCheckProvider} service instances.
     *
     * @return set to {@code false} to disable discovery
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean useSystemServices();

}
