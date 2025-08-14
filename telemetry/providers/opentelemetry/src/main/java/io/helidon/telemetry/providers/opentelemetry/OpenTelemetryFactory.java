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

/**
 * Supplier of {@link Telemetry} using configuration.
 * <p>
 * This factory is typically used when the service registry seeks the telemetry instance.
 * <p>
 * Helidon can prepare the telemetry using the top-level {@code telemetry} config node or using
 * the top-level {@code tracing} config node (for some compatibility with other tracing configurations such as Jaeger and Zipkin).
 * If the configuration contains both, Helidon prefers {@code telemetry} unless the user specifies
 * {@value #USE_HELIDON_TRACING_PROPERTY} as {@code true}.
 */
@Service.Singleton
class OpenTelemetryFactory implements Supplier<Telemetry> {

    private static final System.Logger LOGGER = System.getLogger(OpenTelemetryFactory.class.getName());

    private static final String USE_HELIDON_TRACING_PROPERTY = "io.helidon.telemetry.use-tracing";

    // Temporary flag to select legacy behavior (default for now) or new behavior based on Helidon telemetry.
    @Deprecated(since = "4.3.0", forRemoval = true)
    private static final boolean USE_HELIDON_TRACING_DESPITE_TELEMETRY = Boolean.getBoolean(USE_HELIDON_TRACING_PROPERTY);

    private static final Supplier<String> CONFIG_LOG_MESSAGE_FORMAT = () -> String.format(
            """
                    Configuration contains both top-level 'telemetry' and top-level 'tracing' settings; \
                    using '%%s' because property %s is set to '%b'""",
            USE_HELIDON_TRACING_PROPERTY,
            USE_HELIDON_TRACING_DESPITE_TELEMETRY);

    private final Telemetry telemetry;

    @Service.Inject
    OpenTelemetryFactory(Config config) {
        telemetry = init(chooseSettings(config).build());
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

        if (openTelemetry.prototype().global().orElse(true)) {
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

    private static OpenTelemetryConfig chooseSettings(Config config) {

        /*
        Even though the app includes this telemetry-centric code, if the user specified top-level "tracing" config we use
        that for compatibility unless the user tells us not to.
         */
        Config topLevelTracingConfig = config.get("tracing");
        Config configToUse;
        if (topLevelTracingConfig.exists() && !USE_HELIDON_TRACING_DESPITE_TELEMETRY) {
            configToUse = TracerBuilderConfig.create(topLevelTracingConfig).asConfig();
            if (config.get(Telemetry.CONFIG_KEY).exists()) {
                LOGGER.log(System.Logger.Level.WARNING, String.format(CONFIG_LOG_MESSAGE_FORMAT.get(), "tracing"));
            }
        } else {
            configToUse = config.get(Telemetry.CONFIG_KEY);
            if (config.get("tracing").exists()) {
                LOGGER.log(System.Logger.Level.WARNING, String.format(CONFIG_LOG_MESSAGE_FORMAT.get(), "telemetry"));
            }
        }
        return OpenTelemetryConfig.create(configToUse);
    }

}
