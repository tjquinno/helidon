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

import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;
import io.helidon.metrics.spi.MetersProvider;
import io.helidon.metrics.systemmeters.SystemMetersProvider;

/**
 * Helidon metrics system meters.
 */
@Feature(value = "Built-ins",
         description = "Built-in system meters",
         in = {HelidonFlavor.MP, HelidonFlavor.SE},
         path = {"Metrics", "Builtins"}
)
module io.helidon.metrics.system.meters {
    requires io.helidon.common.features.api;
    requires io.helidon.common;
    requires io.helidon.metrics.api;
    requires java.management;

    provides MetersProvider with SystemMetersProvider;

}