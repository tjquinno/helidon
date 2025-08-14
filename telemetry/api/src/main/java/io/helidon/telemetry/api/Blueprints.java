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

package io.helidon.telemetry.api;

import java.util.List;

import io.helidon.builder.api.Option;

public interface Blueprints {

    /**
     * Config setting blueprints for use by telemetry implementations to accept and converting from neutral settings
     * to implementation-specific ones.
     */
    interface Blueprint {
        /**
         * Whether the telemetry instance should be set as the global isntance.
         *
         * @return true if the instance should be global; false otherwise
         */
        @Option.Configured
        @Option.DefaultBoolean(true)
        boolean global();

        /**
         * Whether telemetry should be enabled.
         *
         * @return true if enabled; false otherwise
         */
        @Option.Configured
        @Option.DefaultBoolean(true)
        boolean enabled();

        /**
         * Service name the telemetry instance should use in exporting data.
         *
         * @return service name
         */
        @Option.Configured
        @Option.Required
        String service();

        /**
         * The propagation types the telemetry instance should use.
         *
         * @return propagation types
         */
        @Option.Configured
        List<String> propagations();

    }
}
