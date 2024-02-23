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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;

import io.helidon.common.Errors;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

/**
 * Test implementation of the config transformer interface.
 * <p>
 *     This implementation roughly parallels what an upstream component might do to adapt some component-specific config keys
 *     to the keys which the OCI metrics integration expects. For example, this transformer maps the overriding key name
 *     {@value ConfigTransformerTest#OVERRIDING_KEY} to {@value ConfigTransformerTest#ORIGINAL_KEY}.
 * <p>
 *     This simple class maps key names only for String-valued leaf nodes.
 * </p>
 */
@Priority(1000) // Anything less than Integer.MAX_VALUE
public class MoreImportantConfigTransformer implements OciMetricsConfigTransformer {

    private static final Map<String, String> MAPPED_CONFIG_KEYS = Map.of(ConfigTransformerTest.OVERRIDING_KEY,
                                                                         ConfigTransformerTest.ORIGINAL_KEY);

    @Override
    public Config apply(Config config) {
        Errors.Collector collector = Errors.collector();

        Map<String, String> mappedConfigItems =
                MAPPED_CONFIG_KEYS.entrySet().stream()
                        .filter(entry -> config.get(entry.getKey()).exists())
                        .map(entry -> new AbstractMap.SimpleEntry<>(entry.getValue(),
                                                                    config.get(entry.getKey())))
                        .collect(HashMap::new,
                                 (map, entry) -> {
                                     if (!entry.getValue().isLeaf()) {
                                         collector.fatal(entry, "is not a leaf node");
                                     } else {
                                         map.put(entry.getKey(), entry.getValue().asString().get());
                                     }
                                 },
                                 Map::putAll);

        Errors errors = collector.collect();
        errors.checkValid();

        return mappedConfigItems.isEmpty() ? config : Config.create(ConfigSources.create(mappedConfigItems),
                                                                    ConfigSources.create(config));
    }
}
