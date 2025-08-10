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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.common.config.Config;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.extension.trace.propagation.OtTracePropagator;

/**
 * Known OpenTelemetry trace context propagation formats.
 */
enum ContextPropagationType {

    /**
     * W3C trace context propagation.
     */
    TRACE_CONTEXT("tracecontext", W3CTraceContextPropagator::getInstance),

    /**
     * W3C baggage propagation.
     */
    BAGGAGE("baggage", W3CBaggagePropagator::getInstance),

    /**
     * Zipkin B3 trace context propagation using a single header.
     */
    B3("b3", B3Propagator::injectingSingleHeader),

    /**
     * Zipkin B3 trace context propagation using multiple headers.
     */
    B3_MULTI("b3multi", B3Propagator::injectingMultiHeaders),

    /**
     * Jaeger trace context propagation format.
     */
    JAEGER("jaeger", JaegerPropagator::getInstance),

    /**
     * OT trace format propagation.
     */
    OT_TRACE("ottrace", OtTracePropagator::getInstance);

    static final String DEFAULT_NAMES = "tracecontext,baggage";

    static final List<ContextPropagationType> DEFAULT = List.of(TRACE_CONTEXT, BAGGAGE);
    static final List<TextMapPropagator> DEFAULT_PROPAGATORS = DEFAULT.stream()
            .map(ContextPropagationType::propagator)
            .collect(Collectors.toCollection(ArrayList::new)); // Used as the config default so must be writeable.

    private final String format;
    private final Supplier<TextMapPropagator> propagatorSupplier;

    ContextPropagationType(String format, Supplier<TextMapPropagator> propagatorSupplier) {
        this.format = format;
        this.propagatorSupplier = propagatorSupplier;
    }

    /**
     * Converts the config node to a {@code PropagationFormat} enum value, using the normal enum mapping plus the
     * OTel-friendly values.
     *
     * @param configNode config node to map
     * @return {@code PropagationFormat} value corresponding to the config node
     */
    static ContextPropagationType from(Config configNode) {
        return configNode.asString()
                .as(ContextPropagationType::from)
                .orElseGet(() -> configNode.as(ContextPropagationType.class).orElseThrow());
    }

    /**
     * Converts the specified string to a {@code PropagationFormat} enum value, using the enum name as well as the
     * OTel-friendly values.
     *
     * @param value string to convert
     * @return {@code PropagationFormat} value corresponding to the provided string
     */
    static ContextPropagationType from(String value) {
        for (ContextPropagationType contextPropagation : ContextPropagationType.values()) {
            if (contextPropagation.format.equals(value) || contextPropagation.name().equals(value)) {
                return contextPropagation;
            }
        }
        throw new IllegalArgumentException("Unknown propagation format: "
                                                   + value
                                                   + "; expected one or more of "
                                                   + Arrays.toString(ContextPropagationType.values()));
    }

    TextMapPropagator propagator() {
        return propagatorSupplier.get();
    }
}
