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
package io.helidon.webserver.cors;

import io.helidon.builder.api.Prototype;

class CorsConfigSupport {

    public static class BuilderDecorator implements Prototype.BuilderDecorator<CorsConfig.BuilderBase<?, ?>> {

        @Override
        public void decorate(CorsConfig.BuilderBase<?, ?> builder) {
            /*
            If config was supplied to the builder but the builder's enabled setting is absent then the config did not explicitly
            set enabled. In that case enable CORS by default (as if enabled: true had been present in the explicit config).
             */
            if (builder.config().isPresent()
                    && builder.config().get().exists()
                    && builder.enabled().isEmpty()) {
                builder.enabled(true);
            }
        }
    }
}
