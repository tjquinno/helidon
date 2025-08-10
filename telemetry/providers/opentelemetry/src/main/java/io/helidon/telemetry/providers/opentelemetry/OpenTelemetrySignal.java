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

import io.helidon.common.config.NamedService;
import io.helidon.service.registry.Service;
import io.helidon.telemetry.Telemetry;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;

/**
 * Behavior common to all Helidon OpenTelemetry signal implementations.
 */
@Service.Contract
public interface OpenTelemetrySignal<T> extends Telemetry.Signal<T>, NamedService {

    /**
     * Applies the signal information to the provided SDK builder, typically invoking a method to assign a signal provider such as
     * {@link io.opentelemetry.sdk.OpenTelemetrySdkBuilder#setTracerProvider(io.opentelemetry.sdk.trace.SdkTracerProvider)}.
     *
     * @param sdkBuilder OpenTelemetry SDK builder to apply the signal information to
     */
    void update(OpenTelemetrySdkBuilder sdkBuilder);

    /**
     * Performs any follow-up work needed for this signal using the build OpenTelemetry SDK.
     *
     * @param sdk the build SDK
     */
    void processSdk(OpenTelemetrySdk sdk);

    /**
     * Performs any clean-up as part of shutting down telemetry.
     */
    void close();

}
