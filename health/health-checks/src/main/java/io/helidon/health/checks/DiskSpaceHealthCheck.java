/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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

package io.helidon.health.checks;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.Locale;
import java.util.function.Consumer;

import io.helidon.config.Config;
import io.helidon.config.mp.DeprecatedMpConfig;
import io.helidon.health.HealthCheckException;
import io.helidon.health.common.BuiltInHealthCheck;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * A health check that verifies whether the server is running out of disk space. This health check will
 * check whether the usage of the disk associated with a specific path exceeds a given threshold. If it does,
 * then the health check will fail.
 * <p>
 * By default, this health check has a threshold of 100%, meaning that it will never fail the threshold check.
 * Also, by default, it will check the root path {@code /}. These defaults can be modified using the
 * {@value CURRENT_CONFIG_KEY_PATH} property (default {@value DEFAULT_PATH}), and the
 * {@value CURRENT_CONFIG_KEY_THRESHOLD_PERCENT}
 * property (default {@value DEFAULT_THRESHOLD}, virtually 100). The threshold should be set to a percent, such as 50 for 50% or
 * 99 for 99%. If disk usage
 * exceeds this threshold, then the health check will fail.
 * </p>
 * <p>
 * Unless ephemeral disk space is being used, it is often not sufficient to simply restart a server in the event
 * that that health check fails.
 *<p>
 * This health check is automatically created and registered through CDI.
 *</p>
 * <p>
 * This health check can be referred to in properties as {@code diskSpace}. So for example, to exclude this
 * health check from being exposed, use {@code health.exclude: diskSpace}.
 * </p>
 */
@Liveness
@ApplicationScoped // this will be ignored if not within CDI
@BuiltInHealthCheck
public class DiskSpaceHealthCheck implements HealthCheck {
    /**
     * Default path on the file system the health check will be executed for.
     * If you need to check a different path (e.g. application runtime disks are not mounted the same
     * directory as application path), use
     * {@link io.helidon.health.checks.DiskSpaceHealthCheck.Builder#path(java.nio.file.Path)}.
     * When running within a MicroProfile server, you can configure path using a configuration key
     * {@value #CURRENT_CONFIG_KEY_PATH}
     * Defaults to {@value}
     */
    public static final String DEFAULT_PATH = ".";
    /**
     * Default threshold percent, when this check starts reporting
     * {@link org.eclipse.microprofile.health.HealthCheckResponse.Status#DOWN}.
     */
    public static final double DEFAULT_THRESHOLD = 99.999;

    static final String CONFIG_KEY_DISKSPACE_PREFIX = "diskSpace";

    static final String CONFIG_KEY_PATH_SUFFIX = "path";
    /**
     * Full configuration key for path, when configured through MicroProfile config.
     *
     * @deprecated The value will change to {@value CURRENT_CONFIG_KEY_PATH} in a future release
     */
    @Deprecated(since = "3.2.11")
    public static final String CONFIG_KEY_PATH = HealthChecks.DEPRECATED_CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX
            + "." + CONFIG_KEY_DISKSPACE_PREFIX
            + "." + CONFIG_KEY_PATH_SUFFIX;
    static final String CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX = "thresholdPercent";
    /**
     * Full configuration key for threshold percent, when configured through Microprofile config.
     *
     * @deprecated The value will change to {@value CURRENT_CONFIG_KEY_THRESHOLD_PERCENT} in future release
     */
    @Deprecated(since = "3.2.11")
    public static final String CONFIG_KEY_THRESHOLD_PERCENT = HealthChecks.DEPRECATED_CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX
            + "." + CONFIG_KEY_DISKSPACE_PREFIX
            + "." + CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX;

    // The following two constants are used in the Javadoc to nudge users toward using the config key prefix "health.checks"
    // rather than the obsolete "helidon.health". Because the original public constants above have always referred to the
    // now-deprecated config prefixes, those values are unchanged to preserve backward compatibility.
    private static final String CURRENT_CONFIG_KEY_PATH = HealthChecks.CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX
            + "." + CONFIG_KEY_DISKSPACE_PREFIX
            + "." + CONFIG_KEY_PATH_SUFFIX;

    private static final String CURRENT_CONFIG_KEY_THRESHOLD_PERCENT = HealthChecks.CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX
            + "." + CONFIG_KEY_DISKSPACE_PREFIX
            + "." + CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX;

    private static final long KB = 1024;
    private static final long MB = 1024 * KB;
    private static final long GB = 1024 * MB;
    private static final long TB = 1024 * GB;
    private static final long PB = 1024 * TB;

    private final double thresholdPercent;
    private final FileStore fileStore;

    // unit tests
    DiskSpaceHealthCheck(FileStore fileStore, double thresholdPercent) {
        this.fileStore = fileStore;
        this.thresholdPercent = thresholdPercent;
    }

    @Inject
    DiskSpaceHealthCheck(org.eclipse.microprofile.config.Config mpConfig) {
        this(builder().update(applyConfig(mpConfig)));
    }

    private DiskSpaceHealthCheck(Builder builder) {
        try {
            this.fileStore = Files.getFileStore(builder.path);
        } catch (IOException e) {
            throw new HealthCheckException("Failed to obtain file store for path " + builder.path.toAbsolutePath(), e);
        }
        this.thresholdPercent = builder.threshold;
    }

    static String format(long bytes) {
        //Formatter ensures that returned delimiter will be always the same
        Formatter formatter = new Formatter(Locale.US);
        if (bytes >= PB) {
            return formatter.format("%.2f PB", bytes / (double) PB).toString();
        } else if (bytes >= TB) {
            return formatter.format("%.2f TB", bytes / (double) TB).toString();
        } else if (bytes >= GB) {
            return formatter.format("%.2f GB", bytes / (double) GB).toString();
        } else if (bytes >= MB) {
            return formatter.format("%.2f MB", bytes / (double) MB).toString();
        } else if (bytes >= KB) {
            return formatter.format("%.2f KB", bytes / (double) KB).toString();
        } else {
            return bytes + " bytes";
        }
    }

    /**
     * A new fluent API builder to configure this health check.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new disk space health check to use, using defaults for all configurable values.
     *
     * @return a new health check to register with
     *         {@link io.helidon.health.HealthSupport.Builder#addLiveness(org.eclipse.microprofile.health.HealthCheck...)}
     * @see #DEFAULT_PATH
     * @see #DEFAULT_THRESHOLD
     */
    public static DiskSpaceHealthCheck create() {
        return builder().build();
    }

    private static Consumer<Builder> applyConfig(org.eclipse.microprofile.config.Config mpConfig) {
        return builder -> {
            DeprecatedMpConfig.getConfigValue(mpConfig,
                                              Path.class,
                                              configKey(HealthChecks.CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX,
                                                        CONFIG_KEY_PATH_SUFFIX),
                                              configKey(HealthChecks.DEPRECATED_CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX,
                                                        CONFIG_KEY_PATH_SUFFIX))
                    .ifPresent(builder::path);
            DeprecatedMpConfig.getConfigValue(mpConfig,
                                              Double.class,
                                              configKey(HealthChecks.CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX,
                                                        CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX),
                                              configKey(HealthChecks.DEPRECATED_CONFIG_KEY_BUILT_IN_HEALTH_CHECKS_PREFIX,
                                                        CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX))
                    .ifPresent(builder::thresholdPercent);
        };
    }

    private static String configKey(String prefix, String suffix) {
        return prefix + "." + CONFIG_KEY_DISKSPACE_PREFIX + "." + suffix;
    }

    @Override
    public HealthCheckResponse call() {
        long diskFreeInBytes;
        long totalInBytes;
        try {
            diskFreeInBytes = fileStore.getUsableSpace();
            totalInBytes = fileStore.getTotalSpace();
        } catch (IOException e) {
            throw new HealthCheckException("Failed to obtain disk space data", e);
        }

        long usedInBytes = totalInBytes - diskFreeInBytes;
        long threshold = (long) ((thresholdPercent / 100) * totalInBytes);

        //Formatter ensures that returned delimiter will be always the same
        Formatter formatter = new Formatter(Locale.US);

        return HealthCheckResponse.named("diskSpace")
                .status(threshold >= usedInBytes)
                .withData("percentFree", formatter.format("%.2f%%", 100 * ((double) diskFreeInBytes / totalInBytes)).toString())
                .withData("free", DiskSpaceHealthCheck.format(diskFreeInBytes))
                .withData("freeBytes", diskFreeInBytes)
                .withData("total", DiskSpaceHealthCheck.format(totalInBytes))
                .withData("totalBytes", totalInBytes)
                .build();
    }

    /**
     * Fluent API builder for {@link io.helidon.health.checks.DiskSpaceHealthCheck}.
     */
    public static final class Builder implements io.helidon.common.Builder<Builder, DiskSpaceHealthCheck> {
        private Path path = Paths.get(DEFAULT_PATH);
        private double threshold = DEFAULT_THRESHOLD;

        private Builder() {
        }

        @Override
        public DiskSpaceHealthCheck build() {
            return new DiskSpaceHealthCheck(this);
        }

        /**
         * Path on the file system to find a file system.
         *
         * @param path path to use
         * @return updated builder instance
         * @see #path(java.nio.file.Path)
         */
        public Builder path(String path) {
            this.path = Paths.get(path);
            return this;
        }

        /**
         * Path on the file system to find a file system.
         *
         * @param path path to use
         * @return updated builder instance
         */
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Threshold percent. When disk is fuller than this percentage, health is switched to down.
         *
         * @param threshold percentage
         * @return updated builder instance
         */
        public Builder thresholdPercent(double threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * Set up the disk space health check via config keys, if present.
         *
         * Configuration options:
         * <table class="config">
         * <caption>Disk space health check configuration</caption>
         * <tr>
         *     <th>Key</th>
         *     <th>Default Value</th>
         *     <th>Description</th>
         *     <th>Builder method</th>
         * </tr>
         * <tr>
         *     <td>{@value CONFIG_KEY_PATH_SUFFIX}</td>
         *     <td>{@value DEFAULT_PATH}</td>
         *     <td>Path for the device for which this health checks available space</td>
         *     <td>{@link #path(Path)} or {@link #path(String)}</td>
         * </tr>
         * <tr>
         *     <td>{@value CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX}</td>
         *     <td>{@value DEFAULT_THRESHOLD}</td>
         *     <td>Minimum percent of disk space consumed for this health check to fail</td>
         *     <td>{@link #thresholdPercent(double)}</td>
         * </tr>
         * </table>
         *
         * @param config {@code Config} node for disk space
         * @return updated builder instance
         */
        public Builder config(Config config) {
            config.get(CONFIG_KEY_PATH_SUFFIX)
                    .as(Path.class)
                    .ifPresent(this::path);

            config.get(CONFIG_KEY_THRESHOLD_PERCENT_SUFFIX)
                    .asDouble()
                    .ifPresent(this::thresholdPercent);

            return this;
        }
    }
}
