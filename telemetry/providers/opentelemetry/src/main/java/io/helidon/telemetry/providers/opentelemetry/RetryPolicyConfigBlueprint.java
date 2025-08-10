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

package io.helidon.telemetry.providers.opentelemetry;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import io.opentelemetry.sdk.common.export.RetryPolicy;

/**
 * OpenTelemetry retry policy settings.
 *
 * @see io.opentelemetry.sdk.common.export.RetryPolicy
 */
@Prototype.Configured
@Prototype.Blueprint(decorator = RetryPolicyConfigSupport.BuilderDecorator.class)
interface RetryPolicyConfigBlueprint {

    /**
     * Maximum number of attempts.
     *
     * @return maximum number of attempts
     */
    @Option.Configured
    Optional<Integer> maxAttempts();

    /**
     * Initial backoff period.
     *
     * @return initial backoff period
     */
    @Option.Configured
    Optional<Duration> initialBackoff();

    /**
     * Maximum backoff period.
     *
     * @return maximum backoff period
     */
    @Option.Configured
    Optional<Duration> maxBackoff();

    /**
     * Backoff multiplier.
     *
     * @return backoff multiplier
     */
    @Option.Configured
    Optional<Double> backoffMultiplier();

    @Option.Access("")
    Optional<RetryPolicy> retryPolicy();
}
