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
package io.helidon.microprofile.metrics;

import java.util.stream.Collectors;

import io.helidon.common.testing.junit5.OptionalMatcher;
import io.helidon.metrics.api.DistributionSetting;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.microprofile.testing.junit5.AddConfig;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

@HelidonTest
@AddConfig(key = "mp.metrics.tags", value=TestMpConfigMapping.TAGS_VALUE)
@AddConfig(key = "mp.metrics.appName", value = TestMpConfigMapping.APP_NAME_VALUE)
@AddConfig(key = "mp.metrics.distribution.histogram.buckets", value = TestMpConfigMapping.BUCKETS_VALUE)
class TestMpConfigMapping {

    static final String TAGS_VALUE = "a=valA,b=valB";
    static final String APP_NAME_VALUE = "myAppName";
    static final String BUCKETS_VALUE = "mySummary=2.0,5.0";

    private static final DistributionSetting.SummaryBuckets[] SUMMARY_BUCKETS = new DistributionSetting.SummaryBuckets[] {
            DistributionSetting.SummaryBuckets.create(BUCKETS_VALUE)};

    @Test
    void checkMappedConfigKeys() {

        MetricsConfig metricsConfig = MetricsFactory.getInstance().metricsConfig();

        assertThat("Tags", metricsConfig.tags().stream()
                           .map(tag -> tag.key() + "=" + tag.value())
                           .collect(Collectors.joining(",")),
                   equalTo(TAGS_VALUE));

        assertThat("App name", metricsConfig.appName(), OptionalMatcher.optionalValue(equalTo(APP_NAME_VALUE)));
        assertThat("Buckets", metricsConfig.summaryBuckets(), contains(SUMMARY_BUCKETS));
    }

}
