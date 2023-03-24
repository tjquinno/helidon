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
package io.helidon.nima.openapi;

import io.helidon.config.metadata.Configured;
import io.helidon.openapi.HelidonOpenApiConfig;

/**
 * Nima-specific extension of the {@link io.helidon.openapi.HelidonOpenApiConfig} interface, declared here
 * primarily for access to protected features of the super-interface.
 */
interface HelidonNimaOpenApiConfig extends HelidonOpenApiConfig {

    /**
     * Creates a new builder for a {@code HelidonNimaOpenApiConfig} instance.
     *
     * @return new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@code HelidonNimaOpenApiConfig} instance.
     */
    class Builder extends HelidonOpenApiConfig.Builder<Builder, HelidonNimaOpenApiConfig> {

        /**
         * Creates a new instance of the builder.
         */
        protected Builder() {
        }

        @Override
        public HelidonNimaOpenApiConfig build() {
            return new ConfigImpl(this);
        }

        /**
         * Implementation of {@code HelidonNimaOpenApiConfig}.
         */
        static class ConfigImpl extends HelidonOpenApiConfig.Builder.ConfigImpl implements HelidonNimaOpenApiConfig {

            /**
             * Creates a new instance of the config implementation.
             *
             * @param builder builder containing settings
             */
            protected ConfigImpl(HelidonNimaOpenApiConfig.Builder builder) {
                super(builder);
            }
        }
    }
}
