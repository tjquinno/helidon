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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface ProcessorBlueprint {

    /**
     * Type of the processor.
     *
     * @return processor type
     */
    Optional<String> type();

    /**
     * Maximum size of data to export as a single batch.
     *
     * @return max export batch size
     */
    Optional<Integer> maxExportBatchSize();

    /**
     * Maximum queue size before triggering a transmission to the collector.
     *
     * @return max queue size
     */
    Optional<Integer> maxQueueSize();

    /**
     * Delay between successive transmissions to the collector.
     *
     * @return scheduling delay
     */
    Optional<Duration> scheduleDelay();

    /**
     * Timeout for the span processor.
     *
     * @return processor timeout
     */
    Optional<Duration> timeout();

    /**
     * Span exporters used by this processor.
     *
     * @return span exporters used by this processor
     */
    List<Exporter> exporters();
}
