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
package io.helidon.metrics.microprofile;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.microprofile.metrics.Metric;

abstract class MpMetric<M extends Meter> implements Metric {

    private final M delegate;
    private final MeterRegistry meterRegistry;

    MpMetric(M delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    M delegate() {
        return delegate;
    }

    /**
     * Returns the meter registry associated with this metric.
     * @return the meter registry
     */
    protected MeterRegistry meterRegistry() {
        return meterRegistry;
    }
}