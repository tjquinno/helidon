/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.metrics.api;

import java.util.function.ToDoubleFunction;

/**
 * A read-only counter which wraps some other object that provides the counter value via a function.
 */
public interface FunctionalCounter extends Meter {

    /**
     * Returns a builder for registering or locating a functional counter.
     *
     * @param name functional counter name
     * @param stateObject object which provides the counter value on demand
     * @param fn function which, when applied to the state object, returns the counter value
     * @return new builder
     * @param <T> type of the state object
     */
    static <T> Builder builder(String name, T stateObject, ToDoubleFunction<T> fn) {
        return MetricsFactory.getInstance().functionalCounterBuilder(name, stateObject, fn);
    }

    /**
     * Returns the counter value.
     *
     * @return counter value
     */
    long count();

    /**
     * Builder for a {@link io.helidon.metrics.api.FunctionalCounter}.
     */
    interface Builder extends Meter.Builder<Builder, FunctionalCounter> {
    }
}