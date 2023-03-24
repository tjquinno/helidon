/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.nima.openapi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.common.LazyValue;
import io.helidon.common.http.Http;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.nima.servicecommon.HelidonFeatureSupport;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.openapi.ExpandedTypeDescription;
import io.helidon.openapi.HelidonOpenApiConfig;
import io.helidon.openapi.OpenAPIMediaType;
import io.helidon.openapi.OpenAPIParser;
import io.helidon.openapi.ParserHelper;
import io.helidon.openapi.Serializer;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;

/**
 * Provides an endpoint and supporting logic for returning an OpenAPI document
 * that describes the endpoints handled by the server.
 * <p>
 * The server can use the {@link OpenApiFeature.Builder} to set OpenAPI-related attributes. If
 * the server uses none of these builder methods and does not provide a static
 * {@code openapi} file, then the {@code /openapi} endpoint responds with a
 * nearly-empty OpenAPI document.
 */
public class OpenApiFeature extends HelidonFeatureSupport {

    /**
     * Default path for serving the OpenAPI document.
     */
    public static final String DEFAULT_WEB_CONTEXT = "/openapi";

    /**
     * Default media type used in responses in absence of incoming Accept
     * header.
     */
    public static final MediaType DEFAULT_RESPONSE_MEDIA_TYPE = MediaTypes.APPLICATION_OPENAPI_YAML;
    private static final String OPENAPI_ENDPOINT_FORMAT_QUERY_PARAMETER = "format";
    private static final System.Logger LOGGER = System.getLogger(OpenApiFeature.class.getName());
    private static final String DEFAULT_STATIC_FILE_PATH_PREFIX = "META-INF/openapi.";
    private static final String OPENAPI_EXPLICIT_STATIC_FILE_LOG_MESSAGE_FORMAT = "Using specified OpenAPI static file %s";
    private static final String OPENAPI_DEFAULTED_STATIC_FILE_LOG_MESSAGE_FORMAT = "Using default OpenAPI static file %s";
    private static final String FEATURE_NAME = "OpenAPI";
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Collections.emptyMap());
    private static final LazyValue<ParserHelper> PARSER_HELPER = LazyValue.create(ParserHelper::create);

    private final ConcurrentMap<Format, String> cachedDocuments = new ConcurrentHashMap<>();
    private final Map<Class<?>, ExpandedTypeDescription> implsToTypes;
    /*
     * To handle the MP case, we must defer constructing the OpenAPI in-memory model until after the server has instantiated
     * the Application instances. By then the builder has already been used to build the OpenAPIFeature object. So save the
     * following raw materials so we can construct the model at that later time.
     */
    private final HelidonOpenApiConfig helidonOpenApiConfig;
    private final OpenApiStaticFile openApiStaticFile;
    private final Lock modelAccess = new ReentrantLock(true);
    private final boolean enabled;
    private OpenAPI model = null;

    /**
     * Creates a new instance of {@code OpenApiFeature}.
     *
     * @param builder the builder to use in constructing the instance
     */
    protected OpenApiFeature(Builder<?, ?> builder) {
        super(LOGGER, builder, FEATURE_NAME);
        enabled = builder.enabled;
        implsToTypes = ExpandedTypeDescription.buildImplsToTypes(PARSER_HELPER.get());
        helidonOpenApiConfig = builder.openApiConfig();
        openApiStaticFile = builder.staticFile();
    }

    /**
     * Creates a new {@link OpenApiFeature.Builder} for {@code OpenAPISupport} using defaults.
     *
     * @return new Builder
     */
    public static Builder<?, ?> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new {@link OpenApiFeature} instance using defaults.
     *
     * @return new OpenAPISUpport
     */
    public static OpenApiFeature create() {
        return builder().build();
    }

    /**
     * Creates a new {@link OpenApiFeature} instance using the
     * 'openapi' portion of the provided
     * {@link io.helidon.config.Config} object.
     *
     * @param config {@code Config} object containing OpenAPI-related settings
     * @return new {@code OpenAPISupport} instance created using the
     *         helidonConfig settings
     */
    public static OpenApiFeature create(Config config) {
        return builder().config(config).build();
    }

    @Override
    public Optional<HttpService> service() {
        return enabled ? Optional.of(this::configureRoutes) : Optional.empty();
    }

    /**
     * Returns the {@code HelidonOpenApiConfig} instance for the feature.
     *
     * @return the Helidon OpenAPI config
     */
    protected HelidonOpenApiConfig helidonOpenApiConfig() {
        return helidonOpenApiConfig;
    }

    /**
     * Triggers preparation of the model from external code.
     */
    protected void prepareModel() {
        model();
    }

    /**
     * Returns a list of {@link org.jboss.jandex.IndexView} instances, one per application, typically derived from annotation
     * scanning (so empty for non-MP impls).
     *
     * @return {@code List} of {@code IndexView}s, one per application
     */
    protected List<? extends IndexView> indexViews() {
        return List.of();
    }

    /**
     * Returns the OpenAPI document in the requested format.
     *
     * @param resultMediaType requested media type
     * @return String containing the formatted OpenAPI document
     *                             from its underlying data
     */
    String prepareDocument(MediaType resultMediaType) {
        OpenAPIMediaType matchingOpenAPIMediaType
                = OpenAPIMediaType.byMediaType(resultMediaType)
                .orElseGet(() -> {
                    LOGGER.log(Level.TRACE,
                               () -> String.format(
                                       "Requested media type %s not supported; using default",
                                       resultMediaType.text()));
                    return OpenAPIMediaType.DEFAULT_TYPE;
                });

        Format resultFormat = matchingOpenAPIMediaType.format();

        return cachedDocuments.computeIfAbsent(resultFormat,
                                                        fmt -> {
                                                            String r = formatDocument(fmt);
                                                            LOGGER.log(Level.TRACE,
                                                                       "Created and cached OpenAPI document in {0} format",
                                                                       fmt.toString());
                                                            return r;
                                                        });
    }

    private void configureRoutes(HttpRules rules) {
        rules.get("/", this::prepareResponse);
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static String typeFromPath(Path path) {
        Path staticFileNamePath = path.getFileName();
        if (staticFileNamePath == null) {
            throw new IllegalArgumentException("File path "
                                                       + path.toAbsolutePath()
                                                       + " does not seem to have a file name value but one is expected");
        }
        String pathText = staticFileNamePath.toString();
        return pathText.substring(pathText.lastIndexOf(".") + 1);
    }

    private static <T> T access(Lock guard, Supplier<T> operation) {
        guard.lock();
        try {
            return operation.get();
        } finally {
            guard.unlock();
        }
    }

    private OpenAPI model() {
        return access(modelAccess, () -> {
            if (model == null) {
                model = prepareModel(helidonOpenApiConfig.openApiConfig(), openApiStaticFile, indexViews());
            }
            return model;
        });
    }

    /**
     * Prepares the OpenAPI model that later will be used to create the OpenAPI
     * document for endpoints in this application.
     *
     * @param openApiConfig       {@code OpenApiConfig} object describing paths, servers, etc.
     * @param staticFile          the static file, if any, to be included in the resulting model
     * @param filteredIndexViews  possibly empty list of FilteredIndexViews to use in harvesting definitions from the code
     * @return the OpenAPI model
     * @throws RuntimeException in case of errors reading any existing static OpenAPI document
     */
    private OpenAPI prepareModel(OpenApiConfig openApiConfig,
                                 OpenApiStaticFile staticFile,
                                 List<? extends IndexView> filteredIndexViews) {
        try {
            // The write lock guarding the model has already been acquired.
            OpenApiDocument.INSTANCE.reset();
            OpenApiDocument.INSTANCE.config(openApiConfig);
            OpenApiDocument.INSTANCE.modelFromReader(OpenApiProcessor.modelFromReader(openApiConfig, getContextClassLoader()));
            if (staticFile != null) {
                OpenApiDocument.INSTANCE.modelFromStaticFile(OpenAPIParser.parse(PARSER_HELPER.get().types(),
                                                                                 staticFile.getContent()));
            }
            if (isAnnotationProcessingEnabled(openApiConfig)) {
                expandModelUsingAnnotations(openApiConfig, filteredIndexViews);
            } else {
                LOGGER.log(Level.DEBUG, "OpenAPI Annotation processing is disabled");
            }
            OpenApiDocument.INSTANCE.filter(OpenApiProcessor.getFilter(openApiConfig, getContextClassLoader()));
            OpenApiDocument.INSTANCE.initialize();

            // Create a copy, primarily to avoid problems during unit testing.
            // The SmallRye MergeUtil omits the openapi value, so we need to set it explicitly.
            return MergeUtil.merge(new OpenAPIImpl(), OpenApiDocument.INSTANCE.get())
                    .openapi(OpenApiDocument.INSTANCE.get().getOpenapi());
        } catch (IOException ex) {
            throw new RuntimeException("Error initializing OpenAPI information", ex);
        }
    }

    private boolean isAnnotationProcessingEnabled(OpenApiConfig config) {
        return !config.scanDisable();
    }

    private void expandModelUsingAnnotations(OpenApiConfig config, List<? extends IndexView> filteredIndexViews) {
        if (filteredIndexViews.isEmpty() || config.scanDisable()) {
            return;
        }

        /*
         * Conduct a SmallRye OpenAPI annotation scan for each filtered index view, merging the resulting OpenAPI models into one.
         * The AtomicReference is effectively final so we can update the actual reference from inside the lambda.
         */
        AtomicReference<OpenAPI> aggregateModelRef = new AtomicReference<>(new OpenAPIImpl()); // Start with skeletal model
        filteredIndexViews.forEach(filteredIndexView -> {
            OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(config, filteredIndexView,
                                                                            List.of(new HelidonAnnotationScannerExtension()));
            OpenAPI modelForApp = scanner.scan();
            if (LOGGER.isLoggable(Level.TRACE)) {

                LOGGER.log(Level.TRACE, String.format("Intermediate model from filtered index view %s:%n%s",
                                                      filteredIndexView.getKnownClasses(),
                                                      formatDocument(Format.YAML, modelForApp)));
            }
            aggregateModelRef.set(
                    MergeUtil.merge(aggregateModelRef.get(), modelForApp)
                            .openapi(modelForApp.getOpenapi())); // SmallRye's merge skips openapi value.

        });
        OpenApiDocument.INSTANCE.modelFromAnnotations(aggregateModelRef.get());
    }

    private void prepareResponse(ServerRequest req, ServerResponse resp) {

        try {
            MediaType resultMediaType = chooseResponseMediaType(req);
            String openAPIDocument = prepareDocument(resultMediaType);
            resp.status(Http.Status.OK_200);
            resp.headers().add(Http.Header.CONTENT_TYPE, resultMediaType.text());
            resp.send(openAPIDocument);
        } catch (Exception ex) {
            resp.status(Http.Status.INTERNAL_SERVER_ERROR_500);
            resp.send("Error serializing OpenAPI document; " + ex.getMessage());
            LOGGER.log(Level.ERROR, "Error serializing OpenAPI document", ex);
        }
    }

    private String formatDocument(Format fmt) {
        return formatDocument(fmt, model());
    }

    private String formatDocument(Format fmt, OpenAPI model) {
        StringWriter sw = new StringWriter();
        Serializer.serialize(PARSER_HELPER.get().types(), implsToTypes, model, fmt, sw);
        return sw.toString();

    }

    private MediaType chooseResponseMediaType(ServerRequest req) {
        /*
         * Response media type default is application/vnd.oai.openapi (YAML)
         * unless otherwise specified.
         */
        Optional<String> queryParameterFormat = req.query()
                .first(OPENAPI_ENDPOINT_FORMAT_QUERY_PARAMETER);
        if (queryParameterFormat.isPresent()) {
            String queryParameterFormatValue = queryParameterFormat.get();
            try {
                return QueryParameterRequestedFormat.chooseFormat(queryParameterFormatValue).mediaType();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Query parameter 'format' had value '"
                                + queryParameterFormatValue
                                + "' but expected " + Arrays.toString(QueryParameterRequestedFormat.values()));
            }
        }

        Optional<MediaType> requestedMediaType = req.headers()
                .bestAccepted(OpenAPIMediaType.preferredOrdering());

        return requestedMediaType
                .orElseGet(() -> {
                    LOGGER.log(Level.TRACE,
                               () -> String.format("Did not recognize requested media type %s; responding with default %s",
                                                   req.headers().acceptedTypes(),
                                                   DEFAULT_RESPONSE_MEDIA_TYPE.text()));
                    return DEFAULT_RESPONSE_MEDIA_TYPE;
                });
    }

    private enum QueryParameterRequestedFormat {
        JSON(MediaTypes.APPLICATION_JSON), YAML(MediaTypes.APPLICATION_OPENAPI_YAML);

        private final MediaType mt;

        QueryParameterRequestedFormat(MediaType mt) {
            this.mt = mt;
        }

        static QueryParameterRequestedFormat chooseFormat(String format) {
            return QueryParameterRequestedFormat.valueOf(format);
        }

        MediaType mediaType() {
            return mt;
        }
    }

    /**
     * Extension we want SmallRye's OpenAPI implementation to use for parsing the JSON content in Extension annotations.
     */
    private static class HelidonAnnotationScannerExtension implements AnnotationScannerExtension {

        @Override
        public Object parseExtension(String key, String value) {
            try {
                return doParseValue(value);
            } catch (Exception ex) {
                LOGGER.log(Level.ERROR, String.format("Error parsing extension key: %s, value: %s", key, value), ex);
                return value;
            }
        }
        @Override
        public Object parseValue(String value) {
            try {
                return doParseValue(value);
            } catch (Exception ex) {
                LOGGER.log(Level.ERROR, String.format("Error parsing value: %s", value), ex);
                return value;
            }
        }

        private Object doParseValue(String value) {
            // Inspired by SmallRye's JsonUtil#parseValue method.
            if (value == null || value.isEmpty()) {
                return null;
            }

            value = value.trim();

            // Try as a boolean.
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.valueOf(value);
            }

            // Try as an integer.
            try {
                return new BigInteger(value);
            } catch (NumberFormatException ex) {
                // Intentionally ignore.
            }

            // Try as a decimal.
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ex) {
                // Intentionally ignore.
            }

            // See if we should parse the value as JSON (structure or array).

            switch (value.charAt(0)) {
            case '{', '[' -> {
                try {
                    JsonReader reader = JSON_READER_FACTORY.createReader(new StringReader(value));
                    JsonValue jsonValue = reader.readValue();
                    return convertJsonValue(jsonValue);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            }

            default -> {
            }
            }

            // Treat as JSON string.
            return value;
        }

        private static Object convertJsonValue(JsonValue jsonValue) {
            switch (jsonValue.getValueType()) {
            case ARRAY -> {
                JsonArray jsonArray = jsonValue.asJsonArray();
                return jsonArray.stream()
                        .map(HelidonAnnotationScannerExtension::convertJsonValue)
                        .collect(Collectors.toList());
            }
            case FALSE -> {
                return Boolean.FALSE;
            }
            case TRUE -> {
                return Boolean.TRUE;
            }
            case NULL -> {
                return null;
            }
            case STRING -> {
                return JsonString.class.cast(jsonValue).getString();
            }
            case NUMBER -> {
                JsonNumber jsonNumber = JsonNumber.class.cast(jsonValue);
                return jsonNumber.numberValue();
            }
            case OBJECT -> {
                JsonObject jsonObject = jsonValue.asJsonObject();
                return jsonObject.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertJsonValue(entry.getValue())));
            }
            default -> {
                return jsonValue.toString();
            }
            }
        }
    }

//    /**
//     * Fluent API builder for {@link OpenApiFeature}.
//     */
//    @Configured(description = "OpenAPI feature configuration")
//    public static class Builder extends AbstractBuilder<Builder, OpenApiFeature> {
//        private Builder() {
//        }
//
//        @Override
//        @ConfiguredOption(type = HelidonOpenApiConfig.class,
//                          mergeWithParent = true)
//        public OpenApiFeature build() {
//            OpenApiFeature openAPIFeature = new OpenApiFeature(this);
//            openAPIFeature.prepareModel();
//            return openAPIFeature;
//        }
//    }

    /**
     * Base builder for OpenAPI service builders, extended by {@link OpenApiFeature.Builder}
     * and MicroProfile implementation.
     *
     * @param <B> type of the builder (subclass)
     * @param <T> type of the built target
     */
//    public abstract static class AbstractBuilder<B extends AbstractBuilder<B, T>, T extends OpenApiFeature>
    @Configured
    public static class Builder<B extends Builder<B, T>, T extends OpenApiFeature>
            extends HelidonFeatureSupport.Builder<B, OpenApiFeature> {

        /**
         * Config key to select the openapi node from Helidon config.
         */
        public static final String CONFIG_KEY = "openapi";

        private HelidonOpenApiConfig.Builder<?, ?> openApiConfigBuilder = HelidonNimaOpenApiConfig.builder();
        private String staticFilePath;
        private boolean enabled = true;

        /**
         * Creates a new builder for an {@code OpenApiFeature}.
         */
        protected Builder() {
            super(DEFAULT_WEB_CONTEXT);
        }

        /**
         * Set various builder attributes from the specified {@code Config} object.
         * <p>
         * The {@code Config} object can specify web-context and static-file in addition to settings
         * supported by {@link io.helidon.openapi.HelidonOpenApiConfig.Builder}.
         *
         * @param config the openapi {@code Config} object possibly containing settings
         * @return updated builder instance
         * @throws NullPointerException if the provided {@code Config} is null
         */
        @ConfiguredOption(type = HelidonOpenApiConfig.class,
                          mergeWithParent = true)
        public B config(Config config) {
            super.config(config);
//            config.get("enabled").asBoolean().ifPresent(this::enabled);
            config.get("static-file")
                    .asString()
                    .ifPresent(this::staticFile);
            openApiConfigBuilder.config(config);
            return identity();
        }

        @Override
        public OpenApiFeature build() {
            return new OpenApiFeature(this);
        }

//        /**
//         * Whether OpenAPI is enabled.
//         *
//         * @param value true/false
//         * @return updated builder
//         */
//        @ConfiguredOption("true")
//        public B enabled(boolean value){
//            enabled = value;
//            return identity();
//        }

        /**
         * File system path of the static OpenAPI document file. Default types are `json`, `yaml`, and `yml`.
         *
         * @param path non-null location of the static OpenAPI document file
         * @return updated builder instance
         */
        @ConfiguredOption(value = DEFAULT_STATIC_FILE_PATH_PREFIX + "*")
        public B staticFile(String path) {
            Objects.requireNonNull(path, "path to static file must be non-null");
            staticFilePath = path;
            return identity();
        }

        /**
         * Sets the builder to use for the characteristics of OpenAPI processing.
         *
         * @param openApiConfigBuilder {@code HelidonOpenApiConfig} builder
         * @return updated builder
         */
        public B openApiConfig(HelidonOpenApiConfig.Builder<?, ?> openApiConfigBuilder) {
            this.openApiConfigBuilder = openApiConfigBuilder;
            return identity();
        }

        /**
         * Makes sure the set-up for OpenAPI is consistent, internally and with
         * the current Helidon runtime environment (MP or non-MP).
         *
         * @return this builder
         * @throws IllegalStateException if validation fails
         */
        protected B validate() throws IllegalStateException {
            return identity();
        }

//        /**
//         * Returns the supplier of index views.
//         *
//         * @return index views supplier
//         */
//        protected Supplier<List<? extends IndexView>> indexViewsSupplier() {
//            // Only in MP can we have possibly multiple index views, one per app, from scanning classes (or the Jandex index).
//            return List::of;
//        }

        /**
         * Returns the HelidonOpenApiConfig instance describing the set-up
         * that will govern the SmallRye OpenAPI behavior.
         *
         * @return {@code HelidonOpenApiConfig} conveying how OpenAPI should behave
         */
        protected HelidonOpenApiConfig openApiConfig() {
            return openApiConfigBuilder.get();
        }

        /**
         * Returns the path to a static OpenAPI document file (if any exists),
         * either as explicitly set using {@link #staticFile(String) }
         * or one of the default files.
         *
         * @return the OpenAPI static file instance for the static file if such
         *         a file exists, null otherwise
         */
        OpenApiStaticFile staticFile() {
            return staticFilePath == null ? getDefaultStaticFile() : getExplicitStaticFile();
        }

        private OpenApiStaticFile getExplicitStaticFile() {
            Path path = Paths.get(staticFilePath);
            String specifiedFileType = typeFromPath(path);
            OpenAPIMediaType specifiedMediaType = OpenAPIMediaType.byFileType(specifiedFileType)
                    .orElseThrow(() -> new IllegalArgumentException("OpenAPI file path "
                                                                            + path.toAbsolutePath()
                                                                            + " is not one of recognized types: "
                                                                            + OpenAPIMediaType.recognizedFileTypes()));
            InputStream is;
            // The SmallRye OpenApiStaticFile needs an *open* InputStream. That code reads and closes the stream.
            try {
                is = new BufferedInputStream(Files.newInputStream(path));
                LOGGER.log(Level.DEBUG,
                           () -> String.format(
                                   OPENAPI_EXPLICIT_STATIC_FILE_LOG_MESSAGE_FORMAT,
                                   path.toAbsolutePath()));
                return new OpenApiStaticFile(is, specifiedMediaType.format());
            } catch (IOException ex) {
                throw new IllegalArgumentException("OpenAPI file "
                                                           + path.toAbsolutePath()
                                                           + " was specified but was not found", ex);
            }
        }

        private OpenApiStaticFile getDefaultStaticFile() {
            List<String> candidatePaths = LOGGER.isLoggable(Level.TRACE) ? new ArrayList<>() : null;
            for (OpenAPIMediaType candidate : OpenAPIMediaType.values()) {
                for (String type : candidate.matchingTypes()) {
                    String candidatePath = DEFAULT_STATIC_FILE_PATH_PREFIX + type;
                    InputStream is = null;
                    try {
                        is = getContextClassLoader().getResourceAsStream(candidatePath);
                        if (is != null) {
                            Path path = Paths.get(candidatePath);
                            LOGGER.log(Level.DEBUG, () -> String.format(
                                    OPENAPI_DEFAULTED_STATIC_FILE_LOG_MESSAGE_FORMAT,
                                    path.toAbsolutePath()));
                            // The SmallRye OpenApiStaticFile needs an *open* InputStream. That code reads and closes the stream.
                            return new OpenApiStaticFile(is, candidate.format());
                        }
                        if (candidatePaths != null) {
                            candidatePaths.add(candidatePath);
                        }
                    } catch (Exception ex) {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ioex) {
                                ex.addSuppressed(ioex);
                            }
                        }
                        throw ex;
                    }
                }
            }
            if (candidatePaths != null) {
                LOGGER.log(Level.TRACE,
                           candidatePaths.stream()
                                   .collect(Collectors.joining(
                                           ",",
                                           "No default static OpenAPI description file found; checked [",
                                           "]")));
            }
            return null;
        }
    }
}
