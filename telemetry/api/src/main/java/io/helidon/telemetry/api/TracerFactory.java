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

import java.util.function.Supplier;

import io.helidon.service.registry.Service;
import io.helidon.tracing.Tracer;

/**
 * Factory for a single global instance of {@link io.helidon.tracing.Tracer}.
 */
@Service.Singleton
class TracerFactory implements Supplier<Tracer> {

    /*
    The legacy OTel tracer implementation used this value for creating the global tracer.
     */
    private static final String GLOBAL_TRACER_NAME = "helidon-service";

    private final Tracer tracer;

    @Service.Inject
    TracerFactory(Telemetry telemetry) {
        tracer = telemetry.signal(Tracer.class)
                .map(s -> s.get(GLOBAL_TRACER_NAME))
                .orElseThrow(() -> new IllegalArgumentException("Unable to find telemetry signal supporting "
                                                                        + Tracer.class.getName()));
    }

    @Override
    public Tracer get() {
        return tracer;
    }
}
