/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.util.Map;
import java.util.TreeMap;

/**
 * Health check response.
 */
public interface HealthCheckResponse {
    /**
     * A new response builder.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Name for the response, typically from the name of the health check.
     *
     * @return health response name
     */
    default String name() {
        return "";
    }

    /**
     * Status of this health check.
     *
     * @return status
     */
    Status status();

    /**
     * Details of this health check.
     * This information will be transferred over the network when details are printed!
     *
     * @return details of this health check
     */
    Map<String, Object> details();

    /**
     * Health check status.
     *
     */
    enum Status {
        /**
         * This health check is fine.
         */
        UP,
        /**
         * This health check failed its precondition.
         */
        DOWN,
        /**
         * This health check failed with an exception that was not expected.
         */
        ERROR;

        /**
         * Returns the poorer of two status values.
         *
         * @param first first status value
         * @param second second status value
         * @return whichever status value is worse
         */
        public static Status poorer(Status first, Status second) {
            // This implementation's use of compareTo relies on the current declaration order of the values as
            // documented on Enum.compareTo. If you change the declaration order you must reimplement this method.
            return first.compareTo(second) > 0 ? first : second;
        }
    }

    /**
     * Fluent API builder for {@link HealthCheckResponse}.
     */
    class Builder implements io.helidon.common.Builder<Builder, HealthCheckResponse> {

        // Use a TreeMap to preserve stability of the details in JSON output.
        private final Map<String, Object> details = new TreeMap<>();
        private Status status = Status.UP;
        private String name;

        private Builder() {
        }

        @Override
        public HealthCheckResponse build() {

            // Use a new map in case the builder is reused and mutated after this  invocation of build().
            return new HealthResponseImpl(this.name, this.status, new TreeMap<>(this.details));
        }

        /**
         * Assigns the name for the health check response.
         *
         * @param name name for the response
         * @return updated builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Status of health check, defaults to {@link HealthCheckResponse.Status#UP}.
         *
         * @param status status
         * @return updated builder
         */
        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        /**
         * Status of health check, defaults to {@link HealthCheckResponse.Status#UP}.
         *
         * @param status status as a boolean ({@code true} for {@link HealthCheckResponse.Status#UP}), ({@code false} for {@link HealthCheckResponse.Status#DOWN})
         * @return updated builder
         */
        public Builder status(boolean status) {
            this.status = status ? Status.UP :  Status.DOWN;
            return this;
        }

        /**
         * Add a detail of this health check, used when details are enabled.
         *
         * @param name  name of the detail
         * @param value value of the detail
         * @return updated builder
         */
        public Builder detail(String name, Object value) {
            this.details.put(name, value);
            return this;
        }


        Map<String, Object> details() {
            return details;
        }

        Status status() {
            return status;
        }
    }
}
