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
package io.helidon.integrations.oci.metrics;

import io.helidon.common.Errors;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigTransformerTest {

    static final String OVERRIDING_KEY = "myNamespace";
    static final String OVERRIDING_VALUE = "my-namespace";

    static final String ORIGINAL_KEY = "namespace";

    @Test
    void checkConfig() {
        OciMetricsSupport.Builder builder = OciMetricsSupport.builder();
        Config originalOciMetricsConfig = Config.just(ConfigSources.classpath("application.yaml"))
                .get(OciMetricsSupport.OCI_METRICS_CONFIG_PREFIX);
        Config ociMetricsConfig = builder.transformConfig(originalOciMetricsConfig);
        // Make sure the overriding key remains present in the config and has the correct value.
        assertThat("myNamespace config", ociMetricsConfig.get(OVERRIDING_KEY).exists(), is(true));
        assertThat("myNamespace config value", ociMetricsConfig.get(OVERRIDING_KEY).asString().get(), is(equalTo((OVERRIDING_VALUE))));

        // Make sure the original key which the overriding one overrides is present and has the overriding value.
        assertThat("Namespace config", ociMetricsConfig.get(ORIGINAL_KEY).exists(), is(true));
        assertThat("Namespace config value", ociMetricsConfig.get(ORIGINAL_KEY).asString().get(), is(equalTo((OVERRIDING_VALUE))));
    }

    @Test
    void checkMappedKeyToNonLeaf() {
        OciMetricsSupport.Builder builder = OciMetricsSupport.builder();
        Config originalOciMetricsConfig = Config.just(ConfigSources.classpath("bad-application.yaml"))
                .get(OciMetricsSupport.OCI_METRICS_CONFIG_PREFIX);
        Errors.ErrorMessagesException ex = assertThrows(Errors.ErrorMessagesException.class,
                                                        () -> builder.transformConfig(originalOciMetricsConfig));

        assertThat("Error message", ex.getMessage(), containsString("not a leaf"));
    }
}
