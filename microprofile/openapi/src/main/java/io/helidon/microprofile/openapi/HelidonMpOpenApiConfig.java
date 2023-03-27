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
package io.helidon.microprofile.openapi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.nima.openapi.OpenApiFeature;
import io.helidon.openapi.HelidonOpenApiConfig;

import org.eclipse.microprofile.openapi.OASConfig;

/**
 * Helidon MP expression of the {@link io.smallrye.openapi.api.OpenApiConfig} interface.
 * <p>
 *     To its super-interface {@link io.helidon.openapi.HelidonOpenApiConfig} this interface adds elements, config keys,
 *     and default values related to annotation scanning. As with the super-interface, this interface follows these
 *     Helidon config naming conventions:
 *     <ul>
 *         <li>using "enabled" instead of "disabled" for certain settings, and</li>
 *         <li>using the descriptive "enabled" instead of the present tense verb "enable".</li>
 *     </ul>
 */
public interface HelidonMpOpenApiConfig extends HelidonOpenApiConfig {

    /**
     * Whether OpenAPI annotation scanning is enabled.
     *
     * @return true/false
     */
    boolean scanEnabled();

    /**
     * Packages to scan.
     *
     * @return {@code Set} of packages to scan
     */
    Set<String> scanPackages();

    /**
     * Classes to scan.
     *
     * @return {@code Set} of classes to scan.
     */
    Set<String> scanClasses();

    /**
     * Packages to exclude from scanning.
     *
     * @return {@code Set} of packages to exclude from scanning
     */
    Set<String> scanExcludePackages();

    /**
     * Classes to exclude from scanning.
     *
     * @return {@code Set} of classes to exclude from scanning
     */
    Set<String> scanExcludeClasses();

    /**
     * Whether to scan for bean validation.
     *
     * @return true/false
     */
    boolean scanBeanValidation();

    /**
     * Whether to scan dependencies.
     *
     * @return true/false
     */
    boolean scanDependenciesEnabled();

    /**
     * Dependency JARs to scan.
     *
     * @return {@code Set} of JARs
     */
    Set<String> scanDependenciesJars();

    /**
     * Profiles to scan.
     *
     * @return {@code Set} of profiles to scan
     */
    Set<String> scanProfiles();

    /**
     * Profiles to exclude from scanning.
     *
     * @return {@code Set} of profiles to exclud from scanning
     */
    Set<String> scanExcludeProfiles();

    /**
     * Creates a new fluid builder for {@code HelidonMpOpenApiConfig}.
     *
     * @return new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for the MP variant of OpenAPI config.
     */
    @Configured
    class Builder extends HelidonOpenApiConfig.Builder<Builder, HelidonMpOpenApiConfig> {

        /**
         * Config key for enabling annotation scanning.
         */
        public static final String SCAN_ENABLED = "scan.enabled";

        /**
         * Config key specifying packages to scan.
         */
        public static final String SCAN_PACKAGES = "scan.packages";

        /**
         * Config key specifying classes to scan.
         */
        public static final String SCAN_CLASSES = "scan.classes";

        /**
         * Config key specifying packages to exclude from scanning.
         */
        public static final String SCAN_EXCLUDE_PACKAGES = "scan.exclude.packages";

        /**
         * Config key specifying classes to exclude from scanning.
         */
        public static final String SCAN_EXCLUDE_CLASSES = "scan.exclude.classes";

        /**
         * Config key specifying whether to perform bean validation scanning.
         */
        public static final String SCAN_BEAN_VALIDATION = "scan.beanvalidation";

        /**
         * Config key specifying whether to disable scanning dependencies.
         */
        public static final String SCAN_DEPENDENCIES_ENABLED = "scan-dependencies.enabled";

        /**
         * Config key specifying dependency JARs to scan.
         */
        public static final String SCAN_DEPENDENCIES_JARS = "scan-dependencies.jars";

        /**
         * Config key specifying scan profiles.
         */
        public static final String SCAN_PROFILES = "scan.profiles";

        /**
         * Config key specifying scan profiles to exclude.
         */
        public static final String SCAN_EXCLUDE_PROFILES = "scan.profiles.exclude";

        /**
         * MicroProfile OpenAPI config key suffix specifying whether scanning is disabled.
         *
         * @see #SCAN_ENABLED
         */
        public static final String SCAN_DISABLE = "scan.disable";

        static final String SCAN_ENABLED_DEFAULT = "true";
        static final String SCAN_DEPENDENCIES_ENABLED_DEFAULT = "true";
        static final String SCAN_BEAN_VALIDATION_DEFAULT = "true";

        private static final Set<String> NEVER_SCAN_PACKAGES = Set.of("java.lang");
        private static final Set<String> NEVER_SCAN_CLASSES = Set.of();
        private static final String USE_JAXRS_SEMANTICS_CONFIG_KEY = "use-jaxrs-semantics";

        private static final String USE_JAXRS_SEMANTICS_FULL_CONFIG_KEY =
                "mp.openapi.extensions.helidon." + USE_JAXRS_SEMANTICS_CONFIG_KEY;
        private static final boolean USE_JAXRS_SEMANTICS_DEFAULT = true;


        private boolean scanEnabled = Boolean.parseBoolean(SCAN_ENABLED_DEFAULT);
        private final Set<String> scanPackages = new HashSet<>();
        private final Set<String> scanClasses = new HashSet<>();
        private final Set<String> scanExcludePackages = new HashSet<>();
        private final Set<String> scanExcludeClasses = new HashSet<>();
        private boolean scanBeanValidation = Boolean.parseBoolean(SCAN_BEAN_VALIDATION_DEFAULT);
        private Boolean scanDependenciesEnabled = Boolean.parseBoolean(SCAN_DEPENDENCIES_ENABLED_DEFAULT);
        private final Set<String> scanDependenciesJars = new HashSet<>();
        private final Set<String> scanProfiles = new HashSet<>();
        private final Set<String> scanExcludeProfiles = new HashSet<>();

        /**
         * Creates a new instance of the builder.
         */
        protected Builder() {
        }

        @ConfiguredOption(key = SCAN_DISABLE, type = Boolean.class)
        @Override
        public Builder config(Config openApiConfigNode) {
            super.config(openApiConfigNode);
            openApiConfigNode.get(SCAN_BEAN_VALIDATION).asBoolean().ifPresent(this::scanBeanValidation);
            openApiConfigNode.get(SCAN_ENABLED).asBoolean().ifPresent(this::scanEnabled);
            openApiConfigNode.get(SCAN_PACKAGES).as(Builder::simpleSet).ifPresent(this::scanPackages);
            openApiConfigNode.get(SCAN_CLASSES).as(Builder::simpleSet).ifPresent(this::scanClasses);
            openApiConfigNode.get(SCAN_EXCLUDE_PACKAGES).as(Builder::simpleSet).ifPresent(this::scanExcludePackages);
            openApiConfigNode.get(SCAN_EXCLUDE_CLASSES).as(Builder::simpleSet).ifPresent(this::scanExcludeClasses);
            openApiConfigNode.get(SCAN_DEPENDENCIES_ENABLED).asBoolean().ifPresent(this::scanDependenciesEnabled);
            openApiConfigNode.get(SCAN_DEPENDENCIES_JARS).as(Builder::simpleSet).ifPresent(this::scanDependenciesJars);
            openApiConfigNode.get(SCAN_PROFILES).asList(String.class).ifPresent(this::scanProfiles);
            openApiConfigNode.get(SCAN_EXCLUDE_PROFILES).asList(String.class).ifPresent(this::scanExcludeProfiles);

            // The MP OpenAPI spec defines several MP config settings. Where possible we defined the keys for this class and
            // its superclass so that they correspond directly to the MP keys (without the "mp.openapi." prefix).
            // Some, though, (currently, one) use "disable" instead of the Helidon conventional "enabled" style. We do not
            // expose methods publically for those, but we need to handle the config settings.
            //
            // The "mp.openapi." prefix is already stripped from the config keys in the openApiConfigNode passed to this method
            // so we use the stripped key to look for the value.
            openApiConfigNode.get(SCAN_DISABLE)
                    .asBoolean()
                    .ifPresent(this::scanDisable);

            return this;
        }

        @Override
        public HelidonMpOpenApiConfig build() {
            scanExcludePackages.addAll(NEVER_SCAN_PACKAGES);
            scanExcludeClasses.addAll(NEVER_SCAN_CLASSES);
            return new MpConfigImpl(this);
        }

        /**
         * Sets whether annotation scanning should be enabled.
         *
         * @param value new setting for annotation scanning enabled flag
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_ENABLED, value = SCAN_ENABLED_DEFAULT)
        public Builder scanEnabled(boolean value) {
            scanEnabled = value;
            return this;
        }

        /**
         * Sets whether bean validation scanning is enabled.
         *
         * @param value true/false
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_BEAN_VALIDATION, value = SCAN_BEAN_VALIDATION_DEFAULT)
        public Builder scanBeanValidation(boolean value) {
            scanBeanValidation = value;
            return this;
        }

        /**
         * Sets whether dependencies scan is enabled.
         *
         * @param value true/false
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_DEPENDENCIES_ENABLED, value = SCAN_DEPENDENCIES_ENABLED_DEFAULT)
        public Builder scanDependenciesEnabled(boolean value) {
            scanDependenciesEnabled = value;
            return this;
        }

        /**
         * Sets the dependency JARs to be scanned.
         *
         * @param value {@code Set} of JAR names to be scanned
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_DEPENDENCIES_JARS)
        public Builder scanDependenciesJars(Set<String> value) {
            scanDependenciesJars.clear();
            scanDependenciesJars.addAll(value);
            return this;
        }

        /**
         * Adds a dependency JAR to be scanned.
         *
         * @param value JAR file name
         * @return updated builder
         */
        public Builder addScanDependenciesJar(String value){
            scanDependenciesJars.add(value);
            return this;
        }

        /**
         * Sets the packages to be scanned.
         *
         * @param packages {@code Set} of package names
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_PACKAGES)
        public Builder scanPackages(Set<String> packages) {
            scanPackages.clear();
            scanPackages.addAll(packages);
            return this;
        }

        /**
         * Adds a package to be scanned.
         *
         * @param packageName name of the package to add for scanning
         * @return updated builder
         */
        public Builder addScanPackage(String packageName) {
            scanPackages.add(packageName);
            return this;
        }

        /**
         * Sets the classes to be scanned.
         *
         * @param classes {@code Set} of class names
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_CLASSES)
        public Builder scanClasses(Set<String> classes) {
            scanClasses.clear();
            scanClasses.addAll(classes);
            return this;
        }

        /**
         * Adds a class to be scanned.
         *
         * @param className name of the class to add for scanning
         * @return updated builder
         */
        public Builder addScanClass(String className) {
            scanClasses.add(className);
            return this;
        }

        /**
         * Sets the packages to exclude from scanning.
         *
         * @param scanExcludePackages {@code Set} of package names to exclude
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_EXCLUDE_PACKAGES)
        public Builder scanExcludePackages(Set<String> scanExcludePackages) {
            this.scanExcludePackages.clear();
            this.scanExcludePackages.addAll(scanExcludePackages);
            return this;
        }

        /**
         * Adds a package to be excluded from scanning.
         *
         * @param packageName name of a package to exclude from scanning
         * @return updated builder
         */
        public Builder addScanExcludePackage(String packageName) {
            scanExcludePackages.add(packageName);
            return this;
        }

        /**
         * Sets the classes to be excluded from scanning.
         *
         * @param scanExcludeClasses {@code Set} of class names to exclude
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_EXCLUDE_CLASSES)
        public Builder scanExcludeClasses(Set<String> scanExcludeClasses) {
            this.scanExcludeClasses.clear();
            this.scanExcludeClasses.addAll(scanExcludeClasses);
            return this;
        }

        /**
         * Adds a class to be excluded from scanning.
         *
         * @param className name of a class to exclude from scanning
         * @return updated builder
         */
        public Builder addScanExcludeClass(String className) {
            scanExcludeClasses.add(className);
            return this;
        }

        /**
         * Sets the profiles to scan.
         *
         * @param scanProfiles {@code List} of profiles to scan
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_PROFILES)
        public Builder scanProfiles(List<String> scanProfiles) {
            this.scanProfiles.clear();
            this.scanProfiles.addAll(scanProfiles);
            return this;
        }

        /**
         * Sets the profiles to exclude from scanning.
         * @param scanProfilesExclude {@code List} of profiles to exclude
         * @return updated builder
         */
        @ConfiguredOption(key = SCAN_EXCLUDE_PROFILES)
        public Builder scanExcludeProfiles(List<String> scanProfilesExclude) {
            this.scanExcludeProfiles.clear();
            this.scanExcludeProfiles.addAll(scanProfilesExclude);
            return this;
        }

        /**
         * Converts the string value (comma-separated sequence) of a config node to a {@code Set<String>}.

         * @param config the config node potentially containing a comma-list string
         * @return {@code Optional} of a {@code Set<String>} from parsing the comma-list
         */
        protected static Set<String> simpleSet(Config config) {
            return config
                    .asString()
                    .map(s -> Arrays.stream(s.split(",")).collect(Collectors.toSet()))
                    .orElse(Set.of());
        }

        private Builder scanDisable(boolean disableSetting) {
            scanEnabled(!disableSetting);
            return this;
        }

        /**
         * MP implementation of the OpenAPI config.
         */
        static class MpConfigImpl extends HelidonOpenApiConfig.Builder.ConfigImpl implements HelidonMpOpenApiConfig {
            private final boolean scanEnabled;
            private final Set<String> scanPackages;
            private final Set<String> scanClasses;
            private final Set<String> scanExcludePackages;
            private final Set<String> scanExcludeClasses;
            private final boolean scanBeanValidation;
            private final boolean scanDependenciesEnabled;
            private final Set<String> scanDependenciesJars;
            private final Set<String> scanProfiles;
            private final Set<String> scanExcludeProfiles;

            private MpConfigImpl(HelidonMpOpenApiConfig.Builder builder) {
                super(builder);
                scanEnabled = builder.scanEnabled;
                scanPackages = builder.scanPackages;
                scanClasses = builder.scanClasses;
                scanExcludePackages = builder.scanExcludePackages;
                scanExcludeClasses = builder.scanExcludeClasses;
                scanBeanValidation = builder.scanBeanValidation;
                scanDependenciesEnabled = builder.scanDependenciesEnabled;
                scanDependenciesJars = builder.scanDependenciesJars;
                scanProfiles = builder.scanProfiles;
                scanExcludeProfiles = builder.scanExcludeProfiles;
            }

            @Override
            public boolean scanEnabled() {
                return scanEnabled;
            }

            @Override
            public Set<String> scanPackages() {
                return scanPackages;
            }

            @Override
            public Set<String> scanClasses() {
                return scanClasses;
            }

            @Override
            public Set<String> scanExcludePackages() {
                return scanExcludePackages;
            }

            @Override
            public Set<String> scanExcludeClasses() {
                return scanExcludeClasses;
            }

            @Override
            public boolean scanBeanValidation() {
                return scanBeanValidation;
            }

            @Override
            public boolean scanDependenciesEnabled() {
                return scanDependenciesEnabled;
            }

            @Override
            public Set<String> scanDependenciesJars() {
                return scanDependenciesJars;
            }

            @Override
            public Set<String> scanProfiles() {
                return scanProfiles;
            }

            @Override
            public Set<String> scanExcludeProfiles() {
                return scanExcludeProfiles;
            }

            // We need the following methods needed to conform to the SmallRye interface.

            @Override
            public boolean scanDisable() {
                return !scanEnabled;
            }

            @Override
            public boolean scanDependenciesDisable() {
                return !scanDependenciesEnabled;
            }

            // End of compatibility methods for the SmallRye interface.

        }
    }
}
