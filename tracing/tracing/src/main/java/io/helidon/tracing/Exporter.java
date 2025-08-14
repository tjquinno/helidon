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

package io.helidon.tracing;

import java.net.URI;
import java.time.Duration;

import io.helidon.common.configurable.Resource;

/**
 * Common behavior of span exporters across telemetry implementations.
 */
public interface Exporter {



    /**
     * Behavior common across builders for an exporter.
     */
    interface Builder extends io.helidon.common.Builder<Builder, Exporter> {

        /**
         * Certificate for the exporter to use in connecting to a secure remote collector.
         *
         * @param clientTlsCert client cert
         * @return updated builder
         */
        Builder clientTlsCert(Resource clientTlsCert);

        /**
         * Private key for the exporter to use in connecting to a secure remote collector.
         *
         * @param clientTlsPrivateKey client key
         * @return updated builder
         */
        Builder clientTlsPrivateKey(Resource clientTlsPrivateKey);

        /**
         * Compression type to use in transmitting span data to the collector.
         *
         * @param compression compression type
         * @return updated builder
         */
        Builder compression(String compression);

        /**
         * Location of the collector.
         *
         * @param endpoint  collector location
         * @return updated builder
         */
        Builder endpoint(URI endpoint);

        // headers/tags

        /**
         * Type of exporter (usage varies among different telemetry implementations).
         *
         * @param type exporter type
         * @return updated builder
         */
        Builder type(String type);

        /**
         * Timeout used in sending trace information to the collector.
         *
         * @param timeout transmission timeout
         * @return updated builder
         */
        Builder timeout(Duration timeout);

        /**
         * Location of trusted certificates for use in connecting to the collector.
         *
         * @param trustedCerts trusted certs
         * @return updated builder
         */
        Builder trustedCerts(Resource  trustedCerts);
    }

}
