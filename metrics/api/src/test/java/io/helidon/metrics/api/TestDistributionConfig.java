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

import java.util.List;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class TestDistributionConfig {

    @Test
    void testExplicitPercentiles() {
        Map<String, String> settings = Map.of("distribution.percentiles",
                                              "alpha.dist-summary=0.3,0.4;alpha.timer=0.5,0.8");
        Config config = Config.just(ConfigSources.create(settings));
        MetricsConfig metricsConfig = MetricsConfig.create(config);
        List<DistributionSetting.Percentiles> percentiles = metricsConfig.percentiles();
        assertThat("Percentiles", percentiles, hasSize(2));
        assertThat("First percentile checking alpha.dist-summary",
                   percentiles.getFirst().matches("alpha.dist-summary"),
                   is(true));
        assertThat("First percentile checking 'other'",
                   percentiles.getFirst().matches("other"),
                   is(false));
        assertThat("First percentile values", percentiles.getFirst().values(), equalTo(new double[] {0.3d, 0.4d}));
    }

    @Test
    void testOverlappingSettingsWithWildcard() {
        Map<String, String> settings = Map.of("distribution.percentiles",
                                              "alpha.*=0.3,0.4;alpha.timer=0.5,0.8");
        Config config = Config.just(ConfigSources.create(settings));
        MetricsConfig metricsConfig = MetricsConfig.create(config);
        assertThat("Using explicit name setting with alpha.timer",
                   metricsConfig.percentiles().getFirst().matches("alpha.timer"),
                   is(true));
        assertThat("Using wildcarded name setting with alpha.timer",
                   metricsConfig.percentiles().get(1).matches("alpha.timer"),
                   is(true));
    }

    @Test
    void testOutOfOrderValues() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                   () -> MetricsConfigSupport.createPercentiles("alpha=0.3,0.2"));
        assertThat("Exception message", ex.getMessage(), containsString("ascending order"));
    }

    @Test
    void testEntryWithNoValues() {
        // Empty list first.
        List<DistributionSetting.Percentiles> percentiles = MetricsConfigSupport.createPercentiles("x=;y=0.4,0.9");
        assertThat("Expected settings", percentiles, hasSize(2));
        assertThat("Value-less percentile setting values length", percentiles.getFirst().values().length, equalTo(0));
        assertThat("Valued percentile setting", percentiles.get(1).values(), equalTo(new double[]{0.4d, 0.9d}));

        // Empty list last
        percentiles = MetricsConfigSupport.createPercentiles("x=0.1,0.2;y=");
        assertThat("Expected settings", percentiles, hasSize(2));
        assertThat("Value-less percentile setting", percentiles.getFirst().values(), equalTo(new double[] {0.1d, 0.2d}));
        assertThat("Valued percentile setting values length", percentiles.get(1).values().length, equalTo(0));
    }

    @Test
    void testAllWithNoValues() {
        List<DistributionSetting.Percentiles> percentiles = MetricsConfigSupport.createPercentiles("*=");
        assertThat("Expected settings", percentiles, hasSize(1));
        assertThat("Value-less percentile setting values length", percentiles.getFirst().values().length, equalTo(0));
        assertThat("Valued percentile setting", percentiles.getFirst().matches("anyName"), is(true));
    }

    @Test
    void testEmptySetting() {
        List<DistributionSetting.Percentiles> percentiles = MetricsConfigSupport.createPercentiles("");
        assertThat("Expected settings", percentiles, hasSize(0));
    }

    @Test
    void testExplicitSummaryBuckets() {
        Map<String, String> settings = Map.of("distribution.summary.buckets",
                                              "alpha.dist-summary=10.0,50.0,100.0;beta.dist-summary=30.0,50.0,123");
        Config config = Config.just(ConfigSources.create(settings));
        MetricsConfig metricsConfig = MetricsConfig.create(config);
        List<DistributionSetting.SummaryBuckets> summaryBuckets = metricsConfig.summaryBuckets();
        assertThat("Summary buckets", summaryBuckets, hasSize(2));
        assertThat("First summary bucket checking beta.dist-summary",
                   summaryBuckets.getFirst().matches("alpha.dist-summary"),
                   is(true));
        assertThat("First summary bucket checking 'other'",
                   summaryBuckets.getFirst().matches("other"),
                   is(false));
        assertThat("First summary bucket values",
                   summaryBuckets.getFirst().values(),
                   equalTo(new double[] {10.0d, 50.0d, 100.0d}));

        assertThat("Second summary bucket values",
                   summaryBuckets.get(1).values(),
                   equalTo(new double[] {30.0d, 50.0d, 123.0d}));
    }

    @Test
    void testOutOfRangeBuckets() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                   () -> DistributionSetting.SummaryBuckets.create("bad=-1.0f,2.0f"));
        assertThat("Exception message", ex.getMessage(), containsString("must be greater than 0"));
    }
}
