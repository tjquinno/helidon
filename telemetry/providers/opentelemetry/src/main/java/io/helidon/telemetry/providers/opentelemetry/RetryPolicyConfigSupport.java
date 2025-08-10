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

import io.helidon.builder.api.Prototype;
import io.helidon.common.LazyValue;

import io.opentelemetry.sdk.common.export.RetryPolicy;

class RetryPolicyConfigSupport {

    static class BuilderDecorator implements Prototype.BuilderDecorator<RetryPolicyConfig.BuilderBase<?, ?>> {

        @Override
        public void decorate(RetryPolicyConfig.BuilderBase<?, ?> target) {

            LazyValue<RetryPolicy.RetryPolicyBuilder> retryPolicyBuilder = LazyValue.create(RetryPolicy::builder);

            target.backoffMultiplier().ifPresent(v -> retryPolicyBuilder.get().setBackoffMultiplier(v));
            target.initialBackoff().ifPresent(v -> retryPolicyBuilder.get().setInitialBackoff(v));
            target.maxAttempts().ifPresent(v -> retryPolicyBuilder.get().setMaxAttempts(v));
            target.maxBackoff().ifPresent(v -> retryPolicyBuilder.get().setMaxBackoff(v));

            if (retryPolicyBuilder.isLoaded()) {
                target.retryPolicy(retryPolicyBuilder.get().build());
            }
        }
    }
}
