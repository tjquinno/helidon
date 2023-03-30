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
package io.helidon.openapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * Helidon OpenAPI settings.
 * <p>
 * The Helidon MP OpenAPI component extends this interface, its implementation, and the interface's builder to reflect
 * the annotation-related behavior which it supports that this interface/implementation/builder do not. Developers of Helidon MP apps
 * with OpenAPI use that interface and builder, if necessary, not these non-MP ones.
 * </p>
 */
public interface HelidonOpenApiConfig {

    /**
     * Strategy for deriving operation ID.
     */
    enum OperationIdStrategy {
        /**
         * Derive operation ID from the method.
         */
        METHOD,

        /**
         * Derive operation ID from the class and method.
         */
        CLASS_METHOD,

        /**
         * Derive operation ID from the package, class, and method.
         */
        PACKAGE_CLASS_METHOD;
    }

    /**
     * Options for handling duplicate operation IDs.
     */
    enum DuplicateOperationIdBehavior {
        /**
         * Fail on duplicate operation IDs.
         */
        FAIL,

        /**
         * Warn on duplicate operationIDs.
         */
        WARN
    }

    /**
     * Fully-qualified class name of the custom model reader.
     *
     * @return model reader class name
     */
    String modelReader();

    /**
     * Fully-qualified class name of the model filter.
     *
     * @return model filter class name
     */
    String filter();

    /**
     * Servers set for the API.
     *
     * @return list of servers
     */
    List<String> servers();

    /**
     * Servers set for the specified path.
     *
     * @param path path of interest
     * @return servers set for the path
     */
    List<String> pathServers(String path);

    /**
     * Servers set for the specified operation ID.
     *
     * @param operationId operation of interest
     * @return servers set for the operation
     */
    List<String> operationServers(String operationId);

    /**
     * Whether array references are enabled.
     *
     * @return true/false
     */
    boolean arrayReferencesEnabled();

    /**
     * Custom class for schema registry.
     *
     * @return class name for custom schema registry
     */
    String customSchemaRegistryClass();

    /**
     * Whether application path support is enabled.
     *
     * @return true/false
     */
    boolean applicationPathEnabled();

    /**
     * Whether private property support is selected.
     *
     * @return true/false
     */
    boolean privatePropertiesEnabled();

    /**
     * Strategy for property naming.
     *
     * @return strategy for property naming
     */
    String propertyNamingStrategy();

    /**
     * Whether property sorting is enabled.
     *
     * @return true/false
     */
    boolean sortedPropertiesEnabled();

    /**
     * {@code Map} from schema name to schema JSON text.
     *
     * @return name-to-schema {@code Map}
     */
    Map<String, String> schemas();

    /**
     * Version of the OpenAPI spec.
     *
     * @return OpenAPI spec version
     */
    String openApiVersion();

    /**
     * Title in the {@code info} section.
     *
     * @return API title
     */
    String infoTitle();

    /**
     * Version in the {@code info} section.
     *
     * @return API version
     */
    String infoVersion();

    /**
     * Description in the {@code info} section.
     *
     * @return API description
     */
    String infoDescription();

    /**
     * Terms of service in the {@code info} section.
     *
     * @return terms of service
     */
    String infoTermsOfService();

    /**
     * Email for the contact in the {@code info} section.
     *
     * @return contact email
     */
    String infoContactEmail();

    /**
     * Name for the contact in the {@code info} section.
     *
     * @return contact name
     */
    String infoContactName();

    /**
     * URL for the contact in the {@code info} section.
     *
     * @return the contact URL
     */
    String infoContactUrl();

    /**
     * Name for the license in the {@code info} section.
     *
     * @return the license name
     */
    String infoLicenseName();

    /**
     * URL for the license in the {@code info} section.
     *
     * @return the URL for the license
     */
    String infoLicenseUrl();

    /**
     * How to derive operation IDs if not specified.
     *
     * @return the operation ID strategy
     */
    OperationIdStrategy operationIdStrategy();

    /**
     * How to handle duplicate operation IDs.
     *
     * @return the duplicate ID behavior value
     */
    DuplicateOperationIdBehavior duplicateOperationIdBehavior();

    /**
     * {@code Optional} of default {@code produces} values for operations.
     *
     * @return {@code Optional}, either empty or wrapping true/false
     */
    Optional<String[]> defaultProduces();

    /**
     * {@code Optional} of default {@code consumes} values for operations.
     *
     * @return {@code Optional}, either empty or wrapping true/false
     */
    Optional<String[]> defaultConsumes();

    /**
     * {@code Optional} of whether to allow a naked path parameter.
     *
     * @return {@code Optional}, either empty or wrapping true/false
     */
    Optional<Boolean> allowNakedPathParameter();

    /**
     * Whether to remove (ignore) schemas that are declared but not used.
     *
     * @return true/false
     */
    boolean removeUnusedSchemas();

    /**
     * Creates a new builder for a {@code HelidonOpenApiConfig} instance.
     *
     * @return new builder
     */
    static Builder<?, HelidonOpenApiConfig> builder() {
        return new Builder.BuilderImpl();
    }

    /**
     * Fluid builder for creating instances of {@link HelidonOpenApiConfig}.
     *
     * <p>
     *     The Helidon MP OpenAPI component has its own interface and builder (which support annotation processing settings)
     *     and they extend their counterparts declared here, so we parameterize the types.
     *
     * @param <B> builder type
     * @param <T> specific type of {@code HelidonOpenApiConfig} created by the builder
     */
    @Configured
    abstract class Builder<B extends Builder<B, T>, T extends HelidonOpenApiConfig> implements io.helidon.common.Builder<B, T> {

        /**
         * Config key prefix for schema overrides for specified classes.
         */
        public static final String SCHEMA = "schema";
        // Key names are inspired by the MP OpenAPI config key names
        /**
         * Config key for model reader.
         */
        public static final String MODEL_READER = "model.reader";
        /**
         * Config key for filter.
         */
        public static final String FILTER = "filter";
        /**
         * Config key for servers.
         */
        public static final String SERVERS = "servers";
        /**
         * Config key for path servers.
         */
        public static final String SERVERS_PATH = "servers.path";
        /**
         * Config key for operation servers.
         */
        public static final String SERVERS_OPERATION = "servers.operation";
        /**
         * Config key prefix for extension settings.
         */
        public static final String EXTENSIONS_PREFIX = "extensions.";
        /**
         * Config key for array referenced enable.
         */
        public static final String ARRAY_REFERENCES_ENABLE = "array-references.enabled";
        /**
         * Config key for custom schema registration class.
         */
        public static final String CUSTOM_SCHEMA_REGISTRY_CLASS = EXTENSIONS_PREFIX + "custom-schema-registry.class";
        /**
         * Config key for application path enabled.
         */
        public static final String APPLICATION_PATH_ENABLED = EXTENSIONS_PREFIX + "application-path.enabled";
        /**
         * Config key for private properties enabled.
         */
        public static final String PRIVATE_PROPERTIES_ENABLED = "private-properties.enabled";
        /**
         * Config key for property naming strategy.
         */
        public static final String PROPERTY_NAMING_STRATEGY = "property-naming-strategy";
        /**
         * Config key for sorted properties enabled.
         */
        public static final String SORTED_PROPERTIES_ENABLED = "sorted-properties.enabled";
        /**
         * Config key for remove unused schemas enable.
         */
        public static final String REMOVE_UNUSED_SCHEMAS_ENABLED = "remove-unused-schemas.enabled";
        /**
         * Config key for OpenAPI version.
         */
        public static final String OPENAPI_VERSION = "openapi.version";
        /**
         * Config key for the info title.
         */
        public static final String INFO_TITLE = "info.title";
        /**
         * Config key for the info version (the version of the API).
         */
        public static final String INFO_VERSION = "info.version";
        /**
         * Config key for the info description.
         */
        public static final String INFO_DESCRIPTION = "info.description";
        /**
         * Config key for the info terms of service.
         */
        public static final String INFO_TERMS_OF_SERVICE = "info.terms-of-service";
        /**
         * Config key for the contact email.
         */
        public static final String INFO_CONTACT_EMAIL = "info.contact.email";
        /**
         * Config key for the contact name.
         */
        public static final String INFO_CONTACT_NAME = "info.contact.name";
        /**
         * Config key for the contact URL.
         */
        public static final String INFO_CONTACT_URL = "info.contact.url";
        /**
         * Config key for the license name.
         */
        public static final String INFO_LICENSE_NAME = "info.license.name";
        /**
         * Config key for the license URL.
         */
        public static final String INFO_LICENSE_URL = "info.license.url";
        /**
         * Config key for the operation ID strategy.
         */
        public static final String OPERATION_ID_STRATEGY = "operation-id-strategy";
        /**
         * Config key for the duplicate operation ID behavior.
         */
        public static final String DUPLICATE_OPERATION_ID_BEHAVIOR = "duplicate-operation-id-behavior";
        /**
         * Config key for the default produces values.
         */
        public static final String DEFAULT_PRODUCES = "default-produces";
        /**
         * Config key for the default consumes values.
         */
        public static final String DEFAULT_CONSUMES = "default-consumes";
        /**
         * Config key for allowing naked path parameters.
         */
        public static final String ALLOW_NAKED_PATH_PARAMETER = "allow-naked-path-parameter";

        /**
         * Default value for array references enable.
         */
        static final String ARRAY_REFERENCES_ENABLED_DEFAULT = "true";
        /**
         * Default value for application path enabled.
         */
        static final String APPLICATION_PATH_ENABLED_DEFAULT = "true";
        /**
         * Default value for property naming strategy.
         */
        static final String PROPERTY_NAMING_STRATEGY_DEFAULT = "IDENTITY";
        /**
         * Default value for private properties enable.
         */
        static final String PRIVATE_PROPERTIES_ENABLED_DEFAULT = "true";
        /**
         * Default value for remove unused schemas.
         */
        static final String REMOVE_UNUSED_SCHEMAS_DEFAULT = "false";
        /**
         * Default value for sorted properties enable.
         */
        static final String SORTED_PROPERTIES_ENABLED_DEFAULT = "false";
        static final DuplicateOperationIdBehavior DUPLICATE_OPERATION_ID_BEHAVIOR_DEFAULT = DuplicateOperationIdBehavior.WARN;
        static final OperationIdStrategy OPERATION_ID_STRATEGY_DEFAULT = null;
        private String modelReader;
        private String filter;
        private final Map<String, List<String>> operationServers = new HashMap<>();
        private final Map<String, List<String>> pathServers = new HashMap<>();
        private final Set<String> servers = new HashSet<>();
        private boolean arrayReferencesEnabled = Boolean.parseBoolean(ARRAY_REFERENCES_ENABLED_DEFAULT);
        private String customSchemaRegistryClass;
        private boolean applicationPathEnabled = Boolean.parseBoolean(APPLICATION_PATH_ENABLED_DEFAULT);
        private boolean privatePropertiesEnabled = Boolean.parseBoolean(PRIVATE_PROPERTIES_ENABLED_DEFAULT);
        private String propertyNamingStrategy = PROPERTY_NAMING_STRATEGY_DEFAULT;
        private boolean sortedPropertiesEnabled = Boolean.FALSE;
        private boolean removeUnusedSchemasEnabled = Boolean.parseBoolean(REMOVE_UNUSED_SCHEMAS_DEFAULT);
        private final Map<String, String> schemas = new HashMap<>();
        private String openApiVersion;
        private String infoTitle;
        private String infoVersion;
        private String infoDescription;
        private String infoTermsOfService;
        private String infoContactEmail;
        private String infoContactName;
        private String infoContactUrl;
        private String infoLicenseName;
        private String infoLicenseUrl;
        private OperationIdStrategy operationIdStrategy = OPERATION_ID_STRATEGY_DEFAULT;
        private DuplicateOperationIdBehavior duplicateOperationIdBehavior = DUPLICATE_OPERATION_ID_BEHAVIOR_DEFAULT;
        private final List<String> defaultProduces = new ArrayList<>();
        private final List<String> defaultConsumes = new ArrayList<>();
        private Optional<Boolean> allowNakedPathParameter = Optional.empty();

        /**
         * Creates a new instance of the builder.
         */
        protected Builder() {
        }

        /**
         * Sets the builder's attributes according to the corresponding entries
         * (if present) in the specified openapi {@link io.helidon.config.Config} object.
         *
         * @param openApiConfigNode {@code} openapi {@code Config} object to addAsObject
         * @return updated builder
         */
        public B config(Config openApiConfigNode) {
            openApiConfigNode.get(MODEL_READER).asString().ifPresent(this::modelReader);
            openApiConfigNode.get(FILTER).asString().ifPresent(this::filter);
            openApiConfigNode.get(SERVERS).as(Builder::listFromCommaSeparatedString).ifPresent(this::servers);
            openApiConfigNode.get(SERVERS_PATH).as(Builder::namedList).ifPresent(this::pathServers);
            openApiConfigNode.get(SERVERS_OPERATION).as(Builder::namedList).ifPresent(this::operationServers);
            openApiConfigNode.get(ARRAY_REFERENCES_ENABLE).asBoolean().ifPresent(this::arrayReferencesEnabled);
            openApiConfigNode.get(CUSTOM_SCHEMA_REGISTRY_CLASS).asString().ifPresent(this::customSchemaRegistryClass);
            openApiConfigNode.get(APPLICATION_PATH_ENABLED).asBoolean().ifPresent(this::applicationPathEnabled);
            openApiConfigNode.get(PRIVATE_PROPERTIES_ENABLED).asBoolean().ifPresent(this::privatePropertiesEnabled);
            openApiConfigNode.get(PROPERTY_NAMING_STRATEGY).asString().ifPresent(this::propertyNamingStrategy);
            openApiConfigNode.get(SORTED_PROPERTIES_ENABLED).asBoolean().ifPresent(this::sortedPropertiesEnabled);
            openApiConfigNode.get(REMOVE_UNUSED_SCHEMAS_ENABLED).asBoolean().ifPresent(this::removeUnusedSchemasEnabled);
            openApiConfigNode.get(SCHEMA).as(Builder::namedSubtreeMap).ifPresent(this::schemas);
            openApiConfigNode.get(OPENAPI_VERSION).asString().ifPresent(this::openApiVersion);
            openApiConfigNode.get(INFO_TITLE).asString().ifPresent(this::infoTitle);
            openApiConfigNode.get(INFO_VERSION).asString().ifPresent(this::infoVersion);
            openApiConfigNode.get(INFO_DESCRIPTION).asString().ifPresent(this::infoDescription);
            openApiConfigNode.get(INFO_TERMS_OF_SERVICE).asString().ifPresent(this::infoTermsOfService);
            openApiConfigNode.get(INFO_CONTACT_EMAIL).asString().ifPresent(this::infoContactEmail);
            openApiConfigNode.get(INFO_CONTACT_NAME).asString().ifPresent(this::infoContactName);
            openApiConfigNode.get(INFO_CONTACT_URL).asString().ifPresent(this::infoContactUrl);
            openApiConfigNode.get(INFO_LICENSE_NAME).asString().ifPresent(this::infoLicenseName);
            openApiConfigNode.get(INFO_LICENSE_URL).asString().ifPresent(this::infoLicenseUrl);
            openApiConfigNode.get(OPERATION_ID_STRATEGY).as(OperationIdStrategy.class).ifPresent(this::operationIdStrategy);
            openApiConfigNode.get(DUPLICATE_OPERATION_ID_BEHAVIOR)
                    .as(DuplicateOperationIdBehavior.class)
                    .ifPresent(this::duplicateOperationIdBehavior);
            openApiConfigNode.get(DEFAULT_PRODUCES).asList(String.class).ifPresent(this::defaultProduces);
            openApiConfigNode.get(DEFAULT_CONSUMES).asList(String.class).ifPresent(this::defaultConsumes);
            openApiConfigNode.get(ALLOW_NAKED_PATH_PARAMETER).asBoolean().ifPresent(this::allowNakedPathParameter);

            return identity();
        }

        /**
         * Developer-provided OpenAPI model reader class name.
         *
         * @param modelReader model reader class name
         * @return updated builder
         */
        @ConfiguredOption(key = MODEL_READER)
        public B modelReader(String modelReader) {
            this.modelReader = modelReader;
            return identity();
        }

        /**
         * Developer-provided OpenAPI filter class name.
         *
         * @param filter filter class name
         * @return updated builder
         */
        @ConfiguredOption
        public B filter(String filter) {
            this.filter = filter;
            return identity();
        }

        /**
         * Alternative servers to service operations.
         *
         * @param operationServers map from operation ID to alternative servers for that operation
         * @return updated builder
         */
        @ConfiguredOption(key = SERVERS_OPERATION + ".*",
                          description = """
                                  Sets alternative servers to service the indicated operation \
                                  (represented here by '*'). \
                                  Repeat for multiple operations.""")
        public B operationServers(Map<String, List<String>> operationServers) {
            this.operationServers.clear();
            this.operationServers.putAll(operationServers);
            return identity();
        }

        /**
         * Set alternative servers to service the specified operation. Repeat for multiple operations.
         *
         * @param operationID      operation ID
         * @param operationServers comma-separated list of servers for the given
         *                         operation
         * @return updated builder
         */
        public B addOperationServers(String operationID, List<String> operationServers) {
            this.operationServers.put(operationID, operationServers);
            return identity();
        }

        /**
         * Whether to enable the application path.
         *
         * @param value true/false
         * @return updated builder
         */
        @ConfiguredOption(APPLICATION_PATH_ENABLED_DEFAULT)
        public B applicationPathEnabled(boolean value) {
            applicationPathEnabled = value;
            return identity();
        }

        /**
         * Alternative servers to service all operations for paths.
         *
         * @param pathServers comma-list of servers for the given path
         * @return updated builder
         */
        @ConfiguredOption(key = SERVERS_PATH + ".*",
                          description = """
                                  Sets alternative servers to service all operations at the indicated path \
                                  (represented here by '*'). \
                                  Repeat for multiple paths.""")
        public B pathServers(Map<String, List<String>> pathServers) {
            this.pathServers.clear();
            this.pathServers.putAll(pathServers);
            return identity();
        }

        /**
         * Add alternative servers to service all operations in the specified path. Repeat for multiple paths.
         *
         * @param path        path for the servers being set
         * @param pathServers comma-list of servers for the given path
         * @return updated builder
         */
        public B addPathServers(String path, List<String> pathServers) {
            this.pathServers.put(path, pathServers);
            return identity();
        }

        /**
         * Servers to serve the endpoints.
         *
         * @param servers comma-list of servers
         * @return updated builder
         */
        @ConfiguredOption
        public B servers(List<String> servers) {
            this.servers.clear();
            this.servers.addAll(servers);
            return identity();
        }

        /**
         * Adds server.
         *
         * @param server server to be added
         * @return updated builder
         */
        public B addServer(String server) {
            servers.add(server);
            return identity();
        }

        /**
         * Schemas for one or more classes referenced in the OpenAPI model.
         *
         * @param schemas map of FQ class name to JSON string depicting the schema
         * @return updated builder
         */
        @ConfiguredOption(key = SCHEMA + ".*",
                          description = """
                                  Sets the schema for the indicated fully-qualified class name (represented here by '*'); \
                                  value is the schema in JSON format. \
                                  Repeat for multiple classes. \
                                  """)
        public B schemas(Map<String, String> schemas) {
            this.schemas.clear();
            this.schemas.putAll(schemas);
            return identity();
        }

        /**
         * Adds a schema for a class.
         *
         * @param fullyQualifiedClassName name of the class the schema describes
         * @param schema                  JSON text definition of the schema
         * @return updated builder
         */
        public B addSchema(String fullyQualifiedClassName, String schema) {
            schemas.put(fullyQualifiedClassName, schema);
            return identity();
        }

        /**
         * Whether array references are enabled.
         *
         * @param arrayReferencesEnabled true/false
         * @return updated builder
         */
        @ConfiguredOption(ARRAY_REFERENCES_ENABLED_DEFAULT)
        public B arrayReferencesEnabled(boolean arrayReferencesEnabled) {
            this.arrayReferencesEnabled = arrayReferencesEnabled;
            return identity();
        }

        /**
         * Custom schema registry class.
         *
         * @param className class to be assigned
         * @return updated builder
         */
        @ConfiguredOption
        public B customSchemaRegistryClass(String className) {
            customSchemaRegistryClass = className;
            return identity();
        }

        /**
         * Sets whether private properties are enabled.
         *
         * @param privatePropertiesEnabled true/false
         * @return updated builder
         */
        @ConfiguredOption(PRIVATE_PROPERTIES_ENABLED_DEFAULT)
        public B privatePropertiesEnabled(boolean privatePropertiesEnabled) {
            this.privatePropertiesEnabled = privatePropertiesEnabled;
            return identity();
        }

        /**
         * Sets the property naming strategy.
         *
         * @param propertyNamingStrategy the strategy to use
         * @return updated builder
         */
        @ConfiguredOption(PROPERTY_NAMING_STRATEGY_DEFAULT)
        public B propertyNamingStrategy(String propertyNamingStrategy) {
            this.propertyNamingStrategy = propertyNamingStrategy;
            return identity();
        }

        /**
         * Sets whether sorted properties are enabled.
         *
         * @param sortedPropertiesEnabled true/false
         * @return updated builder
         */
        @ConfiguredOption(SORTED_PROPERTIES_ENABLED_DEFAULT)
        public B sortedPropertiesEnabled(boolean sortedPropertiesEnabled) {
            this.sortedPropertiesEnabled = sortedPropertiesEnabled;
            return identity();
        }

        /**
         * Sets whether unused schemas should be removed.
         *
         * @param removeUnusedSchemasEnabled true/false
         * @return updated builder
         */
        @ConfiguredOption()
        public B removeUnusedSchemasEnabled(boolean removeUnusedSchemasEnabled) {
            this.removeUnusedSchemasEnabled = removeUnusedSchemasEnabled;
            return identity();
        }

        /**
         * OpenAPI version used in the document.
         *
         * @param openApiVersion version string
         * @return updated builder
         */
        @ConfiguredOption(key = OPENAPI_VERSION)
        public B openApiVersion(String openApiVersion) {
            this.openApiVersion = openApiVersion;
            return identity();
        }

        /**
         * Title of the documented API in the {@code info} section.
         *
         * @param infoTitle title string ({@code info} section)
         * @return updated builder
         */
        @ConfiguredOption(key = INFO_TITLE)
        public B infoTitle(String infoTitle) {
            this.infoTitle = infoTitle;
            return identity();
        }

        /**
         * Version of the API declared in the document.
         *
         * @param infoVersion version string ({@code info} section)
         * @return updated builder
         */
        public B infoVersion(String infoVersion) {
            this.infoVersion = infoVersion;
            return identity();
        }

        /**
         * Description of the documented API in the {@code info} section.
         *
         * @param infoDescription description string ({@code info} section)
         * @return updated builder
         */
        public B infoDescription(String infoDescription) {
            this.infoDescription = infoDescription;
            return identity();
        }

        /**
         * Terms of service for the {@code info} section.
         *
         * @param infoTermsOfService terms of service string ({@code info} section)
         * @return updated builder
         */
        public B infoTermsOfService(String infoTermsOfService) {
            this.infoTermsOfService = infoTermsOfService;
            return identity();
        }

        /**
         * Contact email for the {@code info} section.
         *
         * @param infoContactEmail contact email string ({@code info} section)
         * @return updated builder
         */
        public B infoContactEmail(String infoContactEmail) {
            this.infoContactEmail = infoContactEmail;
            return identity();
        }

        /**
         * Contact name for the {@code info} section.
         *
         * @param infoContactName contact name string ({@code info} section)
         * @return updated builder
         */
        public B infoContactName(String infoContactName) {
            this.infoContactName = infoContactName;
            return identity();
        }

        /**
         * Contact URL for the {@code info} section.
         *
         * @param infoContactUrl contact URL string ({@code info} section)
         * @return updated builder
         */
        public B infoContactUrl(String infoContactUrl) {
            this.infoContactUrl = infoContactUrl;
            return identity();
        }

        /**
         * License name for the {@code info} section.
         *
         * @param infoLicenseName license name string ({@code info} section)
         * @return updated builder
         */
        public B infoLicenseName(String infoLicenseName) {
            this.infoLicenseName = infoLicenseName;
            return identity();
        }

        /**
         * License URL for the {@code info} section.
         *
         * @param infoLicenseUrl license URL string ({@code info} section)
         * @return updated builder
         */
        public B infoLicenseUrl(String infoLicenseUrl) {
            this.infoLicenseUrl = infoLicenseUrl;
            return identity();
        }

        /**
         * How to derive the operation ID.
         *
         * @param operationIdStrategy {@link io.helidon.openapi.HelidonOpenApiConfig.OperationIdStrategy} value
         * @return updated builder
         */
        public B operationIdStrategy(OperationIdStrategy operationIdStrategy) {
            this.operationIdStrategy = operationIdStrategy;
            return identity();
        }

        /**
         * How to respond to duplicate operation IDs.
         *
         * @param duplicateOperationIdBehavior {@link io.helidon.openapi.HelidonOpenApiConfig.DuplicateOperationIdBehavior} value
         * @return updated builder
         */
        public B duplicateOperationIdBehavior(DuplicateOperationIdBehavior duplicateOperationIdBehavior) {
            this.duplicateOperationIdBehavior = duplicateOperationIdBehavior;
            return identity();
        }

        /**
         * Default {@code produces} value(s) for operations.
         *
         * @param defaultProduces media types produced by operations without explicit settings
         * @return updated builder
         */
        public B defaultProduces(List<String> defaultProduces) {
            this.defaultProduces.clear();
            this.defaultProduces.addAll(defaultProduces);
            return identity();
        }

        /**
         * Default {@code consumes} value(s) for operations.
         *
         * @param defaultConsumes media types consumed by operations without explicit settings
         * @return updated builder
         */
        public B defaultConsumes(List<String> defaultConsumes) {
            this.defaultConsumes.clear();
            this.defaultConsumes.addAll(defaultConsumes);
            return identity();
        }

        /**
         * Whether to allow naked path parameters.
         *
         * @param allowNakedPathParameter true/false
         * @return updated builder
         */
        public B allowNakedPathParameter(boolean allowNakedPathParameter) {
            this.allowNakedPathParameter = Optional.of(allowNakedPathParameter);
            return identity();
        }

        /**
         * Builder for non-MP uses of {@code HelidonOpenApiConfig}.
         */
        protected static class BuilderImpl extends Builder<BuilderImpl, HelidonOpenApiConfig> {

            /**
             * Creates a new instance of the builder implementation.
             */
            protected BuilderImpl() {
            }

            @Override
            public HelidonOpenApiConfig build() {
                return new ConfigImpl(this);
            }
        }

        /**
         * Interprets a config node as a container of named list nodes: each child config node's name is the name to apply to the
         * list, and each child's value is a list of strings.
         * <p>
         * For example, the following YAML config yields two named lists, one with name {@code path1} and one with
         * {@code path2}:
         * {@code
         * openapi:
         * servers:
         * path:
         * path1: p1s1,p1s2
         * path2: p2s1,p2s2
         * }
         * </p>
         *
         * @param node the config node to interpret
         * @return {@code Map} from name to a {@code List} of {@code String} values
         */
        protected static Map<String, List<String>> namedList(Config node) {
            Map<String, List<String>> result = new HashMap<>();
            node.ifExists(configNode -> configNode.asNodeList()
                    .get()
                    .forEach(c -> result.put(c.key().name(),
                                             csvToList(c.asString().get()))));
            return result;
        }

        /**
         * Interprets a config node as a tree in which child nodes are name/string-value pairs.
         *
         * @param node the config node to interpret
         * @return {@code Map} from names to their {@code String} values
         */
        protected static Map<String, String> namedSubtreeMap(Config node) {
            //            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            // To suppress token resolution within the subnode (OpenAPI uses $ref, and we do not want
            // config trying to resolve "ref" as a token) create a new node with token resolution off
            // and process that copy.
            Config nodeCopy = Config.builder()
                    .disableKeyResolving()
                    .disableValueResolving()
                    .disableSystemPropertiesSource()
                    .disableEnvironmentVariablesSource()
                    .addSource(ConfigSources.create(node.detach()))
                    .build();

            return nodeCopy.asMap().get();
        }

        /**
         * Converts the string value (comma-separated sequence) of a config node to a {@code List<String>}.
         *
         * @param config the config node potentially containing a comma-list string
         * @return {@code Optional} of a {@code List<String>} from parsing the comma-list
         */
        protected static List<String> listFromCommaSeparatedString(Config config) {
            return config
                    .asString()
                    .map(s -> Arrays.stream(s.split(",")).toList())
                    .orElse(List.of());
        }

        private static List<String> csvToList(String csv) {
            return Arrays.stream(csv.split(","))
                    .toList();
        }

        /**
         * Implementation of {@link HelidonOpenApiConfig} without annotation-related behavior.
         */
        protected static class ConfigImpl implements HelidonOpenApiConfig {

            private final String modelReader;
            private final String filter;
            private final Map<String, List<String>> operationServers;
            private final Map<String, List<String>> pathServers;

            private final List<String> servers;
            private final Boolean arrayReferencesEnabled;
            private final String customSchemaRegistryClass;
            private final Boolean applicationPathEnabled;
            private final Boolean privatePropertiesEnabled;
            private final String propertyNamingStrategy;
            private final Boolean sortedPropertiesEnabled;
            private final Boolean removeUnusedSchemasEnabled;
            private final Map<String, String> schemas;
            private final String openApiVersion;
            private final String infoTitle;
            private final String infoVersion;
            private final String infoDescription;
            private final String infoTermsOfService;
            private final String infoContactEmail;
            private final String infoContactName;
            private final String infoContactUrl;
            private final String infoLicenseName;
            private final String infoLicenseUrl;
            private final HelidonOpenApiConfig.OperationIdStrategy operationIdStrategy;
            private final HelidonOpenApiConfig.DuplicateOperationIdBehavior duplicateOperationIdBehavior;
            private final Optional<String[]> defaultProduces;
            private final Optional<String[]> defaultConsumes;
            private final Optional<Boolean> allowNakedPathParameter;

            /**
             * Creates a new instance of the config implementation.
             *
             * @param builder builder containing settings
             */
            protected ConfigImpl(Builder<?, ?> builder) {
                modelReader = builder.modelReader;
                filter = builder.filter;
                operationServers = builder.operationServers;
                pathServers = builder.pathServers;
                servers = new ArrayList<>(builder.servers);

                arrayReferencesEnabled = builder.arrayReferencesEnabled;
                customSchemaRegistryClass = builder.customSchemaRegistryClass;
                applicationPathEnabled = builder.applicationPathEnabled;
                privatePropertiesEnabled = builder.privatePropertiesEnabled;
                propertyNamingStrategy = builder.propertyNamingStrategy;
                sortedPropertiesEnabled = builder.sortedPropertiesEnabled;
                removeUnusedSchemasEnabled = builder.removeUnusedSchemasEnabled;
                schemas = Collections.unmodifiableMap(builder.schemas);
                openApiVersion = builder.openApiVersion;
                infoTitle = builder.infoTitle;
                infoVersion = builder.infoVersion;
                infoDescription = builder.infoDescription;
                infoTermsOfService = builder.infoTermsOfService;
                infoContactEmail = builder.infoContactEmail;
                infoContactName = builder.infoContactName;
                infoContactUrl = builder.infoContactUrl;
                infoLicenseName = builder.infoLicenseName;
                infoLicenseUrl = builder.infoLicenseUrl;
                operationIdStrategy = builder.operationIdStrategy;
                duplicateOperationIdBehavior = builder.duplicateOperationIdBehavior;
                defaultProduces = toOptArray(builder.defaultProduces);
                defaultConsumes = toOptArray(builder.defaultConsumes);
                allowNakedPathParameter = builder.allowNakedPathParameter;
            }

            @Override
            public String modelReader() {
                return modelReader;
            }

            @Override
            public String filter() {
                return filter;
            }

            @Override
            public List<String> servers() {
                return servers;
            }

            @Override
            public List<String> pathServers(String path) {
                return chooseEntry(pathServers, path);
            }

            @Override
            public List<String> operationServers(String operationID) {
                return chooseEntry(operationServers, operationID);
            }

            @Override
            public boolean arrayReferencesEnabled() {
                return arrayReferencesEnabled;
            }

            @Override
            public String customSchemaRegistryClass() {
                return customSchemaRegistryClass;
            }

            @Override
            public boolean applicationPathEnabled() {
                return applicationPathEnabled;
            }

            @Override
            public boolean privatePropertiesEnabled() {
                return privatePropertiesEnabled;
            }

            @Override
            public String propertyNamingStrategy() {
                return propertyNamingStrategy;
            }

            @Override
            public boolean sortedPropertiesEnabled() {
                return sortedPropertiesEnabled;
            }

            @Override
            public boolean removeUnusedSchemas() {
                return removeUnusedSchemasEnabled;
            }

            @Override
            public Map<String, String> schemas() {
                return schemas;
            }

            @Override
            public String openApiVersion() {
                return openApiVersion;
            }

            public String infoTitle() {
                return infoTitle;
            }

            @Override
            public String infoVersion() {
                return infoVersion;
            }

            @Override
            public String infoDescription() {
                return infoDescription;
            }

            @Override
            public String infoTermsOfService() {
                return infoTermsOfService;
            }

            @Override
            public String infoContactEmail() {
                return infoContactEmail;
            }

            @Override
            public String infoContactName() {
                return infoContactName;
            }

            @Override
            public String infoContactUrl() {
                return infoContactUrl;
            }

            @Override
            public String infoLicenseName() {
                return infoLicenseName;
            }

           @Override
            public String infoLicenseUrl() {
                return infoLicenseUrl;
            }

            @Override
            public HelidonOpenApiConfig.OperationIdStrategy operationIdStrategy() {
                return operationIdStrategy;
            }

            @Override
            public HelidonOpenApiConfig.DuplicateOperationIdBehavior duplicateOperationIdBehavior() {
                return duplicateOperationIdBehavior;
            }

            @Override
            public Optional<String[]> defaultConsumes() {
                return defaultConsumes;
            }

            @Override
            public Optional<String[]> defaultProduces() {
                return defaultProduces;
            }

            @Override
            public Optional<Boolean> allowNakedPathParameter() {
                return allowNakedPathParameter;
            }

            /**
             * Indicates whether annotation scanning is active.
             * <p>
             *     This method is here so the MP-specific subclass can override it and use the MP builder and config
             *     to set its value.
             * </p>
             * @return whether annotation scanning is active
             */
            public boolean scanEnabled() {
                return false;
            }

            private static Optional<String[]> toOptArray(List<String> values) {
                return values.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(values.toArray(new String[0]));
            }

            private static <T, U> List<U> chooseEntry(Map<T, List<U>> map, T key) {
                if (map.containsKey(key)) {
                    return map.get(key);
                }
                return List.of();
            }
        }
    }
}
