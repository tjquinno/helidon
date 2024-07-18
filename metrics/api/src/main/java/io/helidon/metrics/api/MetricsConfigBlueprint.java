/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.config.Config;

/**
 * Configuration settings for metrics.
 *
 * <h2>Scope handling configuration</h2>
 * Helidon allows developers to associate a scope with each meter. The {@value SCOPE_CONFIG_KEY} subsection of the
 * {@value METRICS_CONFIG_KEY} configuration controls
 * <ul>
 *     <li>the default scope value to use if a meter is registered without an explicit scope setting, and</li>
 *     <li>whether and how Helidon records each meter's scope as a tag in the underlying implementation meter registry.
 *     <p>
 *         Specifically, users can specify whether scope tags are used at all and, if so, what tag name to use.
 *     </li>
 * </ul>
 */
@Prototype.Configured(MetricsConfigBlueprint.METRICS_CONFIG_KEY)
@Prototype.Blueprint(decorator = MetricsConfigSupport.BuilderDecorator.class)
@Prototype.CustomMethods(MetricsConfigSupport.class)
interface MetricsConfigBlueprint {

    /**
     * The config key containing settings for all of metrics.
     */
    String METRICS_CONFIG_KEY = "metrics";

    /**
     * Config key for scope-related settings.
     */
    String SCOPE_CONFIG_KEY = "scoping";

    /**
     * Config key for KPI metrics settings.
     */
    String KEY_PERFORMANCE_INDICATORS_CONFIG_KEY = "key-performance-indicators";

    @Deprecated(since = "4.1", forRemoval = true)
    enum GcTimeType { GAUGE, COUNTER }

    @Prototype.FactoryMethod
    static List<Tag> createTags(Config globalTagExpression) {
        return createTags(globalTagExpression.asString().get());
    }

    static List<Tag> createTags(String pairs) {
        // Use a TreeMap to order by tag name.
        Map<String, Tag> result = new TreeMap<>();
        List<String> allErrors = new ArrayList<>();
        String[] assignments = pairs.split("(?<!\\\\),"); // split using non-escaped equals sign
        int position = 0;
        for (String assignment : assignments) {
            List<String> errorsForThisAssignment = new ArrayList<>();
            if (assignment.isBlank()) {
                errorsForThisAssignment.add("empty assignment at position " + position + ": " + assignment);
            } else {
                // Pattern should yield group 1 = tag name and group 2 = tag value.
                Matcher matcher = MetricsConfigSupport.TAG_ASSIGNMENT_PATTERN.matcher(assignment);
                if (!matcher.matches()) {
                    errorsForThisAssignment.add("expected tag=value but found '" + assignment + "'");
                } else {
                    String name = matcher.group(1);
                    String value = matcher.group(2);
                    if (name.isBlank()) {
                        errorsForThisAssignment.add("missing tag name");
                    }
                    if (value.isBlank()) {
                        errorsForThisAssignment.add("missing tag value");
                    }
                    if (!name.matches("[A-Za-z_][A-Za-z_0-9]*")) {
                        errorsForThisAssignment.add(
                                "tag name must start with a letter and include only letters, digits, and underscores");
                    }
                    if (errorsForThisAssignment.isEmpty()) {
                        result.put(name,
                                   // Do not use Tag.create in the next line. That would delegate to the MetricsFactoryManager
                                   // which, ultimately, might try to load config to set up the MetricFactory. But we are
                                   // already trying to load config and that would set up an infinite recursive loop.
                                   NoOpTag.create(name,
                                                  value.replace("\\,", ",")
                                                          .replace("\\=", "=")));
                    }
                }
            }
            if (!errorsForThisAssignment.isEmpty()) {
                allErrors.add(String.format("Position %d with expression %s: %s",
                                            position,
                                            assignment,
                                            errorsForThisAssignment));
            }
            position++;
        }
        if (!allErrors.isEmpty()) {
            throw new IllegalArgumentException("Error(s) in tag expression: " + allErrors);
        }
        return result.values()
                .stream()
                .toList();
    }

    /**
     * Whether metrics functionality is enabled.
     *
     * @return if metrics are configured to be enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Whether to allow anybody to access the endpoint.
     *
     * @return whether to permit access to metrics endpoint to anybody, defaults to {@code true}
     * @see #roles()
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean permitAll();

    /**
     * Hints for role names the user is expected to be in.
     *
     * @return list of hints
     */
    @Option.Configured
    @Option.Default("observe")
    List<String> roles();

    /**
     * Key performance indicator metrics settings.
     *
     * @return key performance indicator metrics settings
     */
    @Option.Configured(KEY_PERFORMANCE_INDICATORS_CONFIG_KEY)
    KeyPerformanceIndicatorMetricsConfig keyPerformanceIndicatorMetricsConfig();

    /**
     * Global tags.
     *
     * @return name/value pairs for global tags
     */
    @Option.Configured
    // for compatibility with MP metrics and earlier Helidon releases
    List<Tag> tags();

    /**
     * Value for the application tag to be added to each meter ID.
     *
     * @return application tag value
     */
    @Option.Configured
    Optional<String> appName();

    /**
     * Name for the application tag to be added to each meter ID.
     *
     * @return application tag name
     */
    @Option.Configured
    Optional<String> appTagName();

    /**
     * Settings related to scoping management.
     *
     * @return scoping settings
     */
    @Option.Configured
    ScopingConfig scoping();

    /**
     * Whether automatic REST request metrics should be measured.
     *
     * @return true/false
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean restRequestEnabled();

    /**
     * Percentile settings of the form {@code meter-name-expression=percentile-values} where {@code meter-name-expression} is
     * a name of a distribution summary or timer or a name prefix followed by a {@code *} suffix, and {@code percentile-values} is
     * a possibly empty comma-separated list of decimal values in the closed interval {@code [0.0, 1.0]}. A builder for a
     * distribution summary or timer which matches the {@code name-expression} is preloaded with the configured percentile values.
     * If a builder's name matches multiple settings the last match wins.
     * <p>
     *     The string form of the configuration value can contain multiple occurrences of this format separated by semicolons.
     * <p>
     * Example: {@code distribution.percentiles=alpha.*=0.5,0.75;alpha.summary=0.3,0.4} assigns percentile values 0.5 and 0.75
     * to distribution summary or timer builders with names starting with {@code alpha.} and assigns values 0.3 and 0.4 to a
     * builder with the exact meter name {@code alpha.summary}.
     * <p>
     *     Example: {@code distribution.percentiles=delta.summary=} suppresses percentiles for {@code delta.summary}.
     * <p>
     *     For any builder with a name that matches none of the settings the Helidon Micrometer-based metrics implementation
     *     preloads default percentile values of 0.5, 0.75, 0.95, 0.98, 0.99, 0.999.
     * </p>
     * @return percentile settings
     */
    @Option.Configured(value = "distribution.percentiles")
    List<DistributionSetting.Percentiles> percentiles();

    /**
     * Timer distribution summary (histogram) bucket settings of the form {@code meter-name-expression=timer-bucket-values} where
     * {@code meter-name-expression} is a name of a timer or a name prefix followed by a {@code *} suffix, and
     * {@code timer-bucket-values} is a possibly empty comma-separated list of integer values each followed by a time unit suffix:
     * <ul>
     *     <li>{@code ms} milliseconds (the default if no suffix appears after a value</li>
     *     <li>{@code s} seconds</li>
     *     <li>{@code m} minutes</li>
     *     <li>{@code h} hours</li>
     * </ul>
     * A builder for a timer which matches the {@code name-expression} is preloaded with the configured bucket boundary values.
     * If a builder's name matches multiple settings the last match wins.
     * <p>
     *     The string form of the configuration value can contain multiple occurrences of this format separated by semicolons.
     * <p>
     *     Example: {@code distribution.timer.buckets=alpha.*=500ms,2s,3m;alpha.other=10s,2m,5h} assigns bucket values 500 ms,
     *     2 seconds, and 3 minutes to timer builders with names starting with {@code alpha.} and assigns values 10 seconds,
     *     2 minutes, and 5 hours to a timer builder with the exact meter name {@code alpha.other}.
     * </p>
     * @return timer distribution summary bucket settings
     */
    @Option.Configured(value = "distribution.timer.buckets")
    List<DistributionSetting.TimerBuckets> timerBuckets();

    /**
     * Non-timer distribution summary (histogram) bucket settings of the form {@code meter-name-expression=summary-bucket-values}
     * where {@code meter-name-expression} is a name of a distribution summary or a name prefix followed by a {@code *} suffix,
     * and {@code summary-bucket-values} is a possibly empty comma-separated list of decimal or integer numbers all greater than
     * zero.
     * A builder for a distribution summary which matches the {@code name-expression} is preloaded with the configured bucket
     * boundary values.
     * If a builder's name matches multiple settings the last match wins.
     * <p>
     *     The string form of the configuration value can contain multiple occurrences of this format separated by semicolons.
     * <p>
     *     Example: {@code distribution.summary.buckets=alpha.*=10.0,50.0,100.0;beta.summary1=30.0,50.0,123} assigns bucket
     *     values 10.0, 50.0, and 100.0 to builders with names starting with {@code alpha.}, and assigns bucket values
     *     30.0, 50.0, and 123.0 to any builder with the exact meter name {@code beta.summary1}.
     * </p>
     * @return non-timer distribution summary bucket settings
     */
    @Option.Configured(value = "distribution.summary.buckets")
    List<DistributionSetting.SummaryBuckets> summaryBuckets();

    /**
     * Metrics configuration node.
     *
     * @return metrics configuration
     */
    @Option.Redundant
    Config config();

    /**
     * Whether the {@code gc.time} meter should be registered as a gauge (vs. a counter).
     * The {@code gc.time} meter is inspired by the MicroProfile Metrics spec, in which the meter was originally checked to
     * be a counter but starting in 5.1 was checked be a gauge. For the duration of Helidon 4.x users can choose which
     * type of meter Helidon registers for {@code gc.time}.
     * @return the type of meter to use for registering {@code gc.time}
     * @deprecated Provided for backward compatibility only; no replacement
     */
    @Deprecated(since = "4.1", forRemoval = true)
    @Option.Configured
    @Option.Default("COUNTER")
    GcTimeType gcTimeType();

    /**
     * Reports whether the specified scope is enabled, according to any scope configuration that
     * is part of this metrics configuration.
     *
     * @param scope scope name
     * @return true if the scope as a whole is enabled; false otherwise
     */
    boolean isScopeEnabled(String scope);

    /**
     * Determines whether the meter with the specified name and within the indicated scope is enabled.
     *
     * @param name  meter name
     * @param scope scope name
     * @return whether the meter is enabled
     */
    boolean isMeterEnabled(String name, String scope);
}
