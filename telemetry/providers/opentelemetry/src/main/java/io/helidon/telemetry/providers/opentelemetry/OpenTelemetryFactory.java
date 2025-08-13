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

package io.helidon.telemetry.providers.opentelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.helidon.common.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.telemetry.api.Telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;

@Service.Singleton
class OpenTelemetryFactory implements Supplier<Telemetry> {

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryFactory.class.getName());

    // Temporary flag to select legacy behavior (default for now) or new behavior based on Helidon telemetry.
    private static final boolean USE_HELIDON_TELEMETRY = Boolean.getBoolean("io.helidon.tracing.use-telemetry");

    private final Telemetry telemetry;

    @Service.Inject
    OpenTelemetryFactory(Config config) {

//        telemetry = init(chooseSettings(config).build());
        telemetry = init(OpenTelemetryConfig.create(config.get(Telemetry.CONFIG_KEY)).build());
    }

    static List<String> otelReasonsForUsingAutoConfig() {
        List<String> reasons = new ArrayList<>();
        if (Boolean.getBoolean("otel.java.global-autoconfigure.enabled")) {
            reasons.add(
                    "OpenTelemetry global autoconfigure is enabled using the system property otel.java.global-autoconfigure"
                            + ".enabled");
        }
        String envvar = System.getenv("OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED");
        if (envvar != null && envvar.equals("true")) {
            reasons.add(
                    "OpenTelemetry global autoconfigure is enabled using the environment variable "
                            + "OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED");
        }
        return reasons;
    }

    @Override
    public Telemetry get() {
        return telemetry;
    }

    private static HelidonOpenTelemetry init(HelidonOpenTelemetry openTelemetry) {

        var prototype = openTelemetry.prototype();

        var ot = prototype.openTelemetry();

        if (prototype.global()) {
            List<String> otelReasonsForUsingAutoConfig = otelReasonsForUsingAutoConfig();
            if (!otelReasonsForUsingAutoConfig.isEmpty()) {
                if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                    LOGGER.log(System.Logger.Level.TRACE,
                               "Using OTel autoconfigure: " + otelReasonsForUsingAutoConfig);
                }
                ot = GlobalOpenTelemetry.get();

            } else {
                GlobalOpenTelemetry.set(ot);
            }
        }

        var finalOt = ot;
        prototype.signals().forEach(s -> s.openTelemetry(finalOt));

        return openTelemetry;
    }

//    @Deprecated(since = "4.3.0", forRemoval = true)
//    private static OpenTelemetryConfig chooseSettings(Config config) {
//        /*
//        Even though the app includes this telemetry-centric code, if the user specified top-level "tracing" config we use
//        that for compatibility unless the user tells us not to.
//         */
//
//        if (config.get("tracing").exists() && !USE_HELIDON_TELEMETRY) {
//            Services.get()
//        }
//        OpenTelemetryConfig.create(config.get(Telemetry.CONFIG_KEY)
//    }


}
