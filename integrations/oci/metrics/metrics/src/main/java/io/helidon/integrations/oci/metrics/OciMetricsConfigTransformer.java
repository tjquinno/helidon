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

import java.util.function.Function;

import io.helidon.config.Config;

/**
 * A supplier of {@link io.helidon.config.Config} for use by the OCI metrics integration library.
 * <p>
 *     If an upstream components that uses this library uses different config keys from what this component expects,
 *     it can implement this interface to transform the {@code Config} which contains the unexpected keys
 *     into the keys which this component expects.
 * </p>
 */
public interface OciMetricsConfigTransformer extends Function<Config, Config> {
}
