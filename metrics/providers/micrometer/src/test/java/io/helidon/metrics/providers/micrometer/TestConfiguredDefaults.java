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
package io.helidon.metrics.providers.micrometer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Timer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItems;

class TestConfiguredDefaults {

    // For convenience in testing that the "default defaults" are used when no configured settings apply.
    private static final Double[] DEFAULT_PERCENTILES_BOXED = Arrays
            .stream(MDistributionStatisticsConfig.Builder.DEFAULT_PERCENTILES)
            .boxed()
            .toArray(Double[]::new);

    @Test
    void testTimerDefaulting() {
        Config config = Config.just(
                ConfigSources.create(Map.of("distribution.percentiles",
                                            "alpha.histogram=0.3,0.4;alpha.timer=0.5,0.8;gamma.*=",
                                            "distribution.summary.buckets",
                                            "alpha.histogram=10.0,50.0,100.0;beta.histogram=30.0,50.0,123",
                                            "distribution.timer.buckets",
                                            "alpha.timer=500ms,2s,3m;beta.timer=10s,2m,5h")));

        // Invoking getInstance(config) creates a new MetricsFactory and also sets it to the current one so it will be used
        // by the later Timer.builder(name) invocation.
        MetricsFactory mf = MetricsFactory.getInstance(config);

        Timer.Builder timerBuilder = Timer.builder("alpha.timer");

        assertThat("alpha timer builder percentiles", timerBuilder.percentiles(), hasItems(0.5d, 0.8d));
        assertThat("alpha timer builder buckets", timerBuilder.buckets(), hasItems(Duration.ofMillis(500),
                                                                             Duration.ofSeconds(2),
                                                                             Duration.ofMinutes(3)));

        timerBuilder.buckets(Duration.ofHours(4));
        assertThat("alpha timer builder buckets after update", timerBuilder.buckets(), hasItems(Duration.ofHours(4)));

        timerBuilder = Timer.builder("beta.timer");

        // Apart from the configuration, there are hard-coded percentile defaults that we should see here.
        assertThat("beta timer builder percentiles",
                   timerBuilder.percentiles(),
                   contains(DEFAULT_PERCENTILES_BOXED));

        timerBuilder = Timer.builder("gamma.timer");
        // The config should set no percentiles for this meter, suppressing the otherwise-defaulted values.
        assertThat("gamme timer builder percentiles",
                   timerBuilder.percentiles(),
                   emptyIterable());
    }
}
