/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.metrics.api;

import java.time.Duration;

/**
 * Common behavior for the various subtypes of distribution-related configuration (e.g., percentiles, dist. summary
 * buckets, and timer buckets).
 * <p>
 *     Each {@code DistributionSetting} instance supports a single meter name expression and zero or more values. The intent
 *     is that any meter builder with a name that matches the setting's name expression will use the values associated with
 *     that expression as the default values. Currently, all subtypes require that the values list be in increasing order.
 * </p>
 *
 * @param <T> type of values exposed by a particular subtype
 */
public interface DistributionSetting<T extends Comparable<T>> {

    /**
     * Distribution settings for percentiles - expressed as double values in the range [0.0, 1.0].
     */
    interface Percentiles extends DistributionSetting<Double> {

        /**
         * Creates a new setting group for percentiles.
         *
         * @param nameExpressionWithValues name expression plus values
         * @return new percentiles instance
         */
        static Percentiles create(String nameExpressionWithValues) {
            return new DistributionSettingImpl.PercentilesImpl(nameExpressionWithValues);
        }

        /**
         * Returns the configured percentile values.
         *
         * @return configured percentile values
         */
        double[] values();
    }

    /**
     * Distribution settings for distribution summary buckets - expressed as double and integer values greater than or equal to 0.
     */
    interface SummaryBuckets extends DistributionSetting<Double> {

        /**
         * Creates a new setting group for distribution summary buckets.
         *
         * @param nameExpressionWithValues name expression plus values
         * @return new distribution summary buckets instance
         */
        static SummaryBuckets create(String nameExpressionWithValues) {
            return new DistributionSettingImpl.SummaryBucketsImpl(nameExpressionWithValues);
        }

        /**
         * Returns the configured distribution summary bucket values.
         *
         * @return configured distribution summary bucket values
         */
        double[] values();
    }

    /**
     * Distribution settings for timer buckets - expressed as integer values with one of the following suffixes:
     * <ul>
     *     <li>ms - milliseconds</li>
     *     <li>s - seconds</li>
     *     <li>m - minutes</li>
     *     <li>h - hours</li>
     *     <li>[none] - milliseconds.</li>
     * </ul>
     */
    interface TimerBuckets extends DistributionSetting<Duration> {

        /**
         * Creates a new setting group for timer buckets.
         *
         * @param nameExpressionWithValues name expression plus values
         * @return new timer buckets instance
         */
        static TimerBuckets create(String nameExpressionWithValues) {
            return new DistributionSettingImpl.TimerBucketsImpl(nameExpressionWithValues);
        }

        /**
         * Returns the configured timer bucket values.
         *
         * @return configured timer bucket values
         */
        Duration[] values();
    }

    /**
     * Returns whether the specified candidate meter name matches the name expression used to initialize this
     * distribution setting.
     *
     * @param name candidate meter name
     * @return true if the candidate meter name matches this setting's name expression; false otherwise
     */
    boolean matches(String name);
}
