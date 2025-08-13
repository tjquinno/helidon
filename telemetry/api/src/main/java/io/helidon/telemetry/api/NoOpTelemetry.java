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
import java.util.Optional;

class NoOpTelemetry implements Telemetry {

    static final Telemetry.Builder BUILDER = new Telemetry.Builder() {

        @Override
        public Builder service(String service) {
            return this;
        }

        @Override
        public Builder enabled(boolean enabled) {
            return this;
        }

        @Override
        public Builder global(boolean global) {
            return this;
        }

        @Override
        public Builder propagations(List<String> propagation) {
            return this;
        }

        @Override
        public Telemetry build() {
            return new NoOpTelemetry();
        }
    };


    private NoOpTelemetry() {
    }

    @Override
    public <T> Optional<Signal<T>> signal(Class<T> signalType) {
        return Optional.empty();
    }

    @Override
    public <B> B unwrap(Class<B> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap " + this.getClass().getName() + " as " + type);
    }

    @Override
    public void close() {
    }
}
