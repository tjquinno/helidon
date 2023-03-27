/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.microprofile.server.JaxRsApplication;
import io.helidon.microprofile.server.JaxRsCdiExtension;
import io.helidon.nima.openapi.OpenApiFeature;
import io.helidon.openapi.HelidonOpenApiConfig;

import io.smallrye.openapi.api.OpenApiConfig;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.jandex.IndexView;

/**
 * MP variant of {@link io.helidon.nima.openapi.OpenApiFeature} which supports annotation scanning.
 */
class MpOpenApiFeature extends OpenApiFeature {

    /**
     * Creates a fluid builder for an {@code MpOpenApiFeature} instance.
     *
     * @return new builder
     */
    static Builder mpBuilder() {
        return new Builder();
    }

    private static final System.Logger LOGGER = System.getLogger(MpOpenApiFeature.class.getName());
    private final IndexViewHelper indexViewHelper;

    protected MpOpenApiFeature(Builder builder) {
        super(builder);
        // Create the helper now to use the builder settings. We use it only later once we have the config.

        indexViewHelper = new IndexViewHelper(super.helidonOpenApiConfig(),
                                              builder.singleIndexViewSupplier,
                                              builder.useJaxRsSemantics);
    }

    // For visibility to the CDI extension
    @Override
    protected void prepareModel() {
        super.prepareModel();
    }

    @Override
    protected List<? extends IndexView> indexViews() {
        return indexViewHelper.perAppFilteredIndexViews();
    }

    /**
     * Returns the {@code JaxRsApplication} instances that should be run, according to the JAX-RS CDI extension.
     *
     * @return List of JaxRsApplication instances that should be run
     */
    static List<JaxRsApplication> jaxRsApplicationsToRun() {
        JaxRsCdiExtension ext = CDI.current()
                .getBeanManager()
                .getExtension(JaxRsCdiExtension.class);

        return ext.applicationsToRun();
    }

    @Configured
    static final class Builder extends OpenApiFeature.Builder<Builder, MpOpenApiFeature> {

        // This is the prefix users will use in the config file.
        static final String MP_OPENAPI_CONFIG_PREFIX = "mp." + OpenApiFeature.Builder.CONFIG_KEY;

        private static final String USE_JAXRS_SEMANTICS_CONFIG_KEY = "use-jaxrs-semantics";

        static final String USE_JAXRS_SEMANTICS_FULL_CONFIG_KEY =
                MP_OPENAPI_CONFIG_PREFIX + ".extensions.helidon." + USE_JAXRS_SEMANTICS_CONFIG_KEY;
        private static final boolean USE_JAXRS_SEMANTICS_DEFAULT = true;

        private static final System.Logger LOGGER = System.getLogger(Builder.class.getName());

        /*
         * Provided by the OpenAPI CDI extension for retrieving a single IndexView of all scanned types for the single-app or
         * synthetic app cases.
         */
        private Supplier<? extends IndexView> singleIndexViewSupplier;

        private boolean useJaxRsSemantics = USE_JAXRS_SEMANTICS_DEFAULT;

        Builder() {
            super.openApiConfig(HelidonMpOpenApiConfig.builder());
        }

        Builder singleIndexViewSupplier(Supplier<? extends IndexView> singleIndexViewSupplier) {
            this.singleIndexViewSupplier = singleIndexViewSupplier;
            return this;
        }

        @Override
        public MpOpenApiFeature build() {
            // We need to defer some work until later when the server has started the applications.
            return new MpOpenApiFeature(this);
        }

        @Override
        @ConfiguredOption(type = HelidonMpOpenApiConfig.class, mergeWithParent = true)
        public Builder openApiConfig(HelidonOpenApiConfig.Builder<?, ?> openApiConfigBuilder) {
            super.openApiConfig(openApiConfigBuilder);
            return this;
        }

        /**
         * Assigns various OpenAPI settings from the specified MP OpenAPI {@code Config} object.
         *
         * @param config the OpenAPI {@code Config} object possibly containing settings
         * @return updated builder instance
         */
        @ConfiguredOption(type = HelidonMpOpenApiConfig.class, mergeWithParent = true)
        public Builder config(Config config) {

            super.config(config);

            // use-jaxrs-semantics is intended for Helidon's private use in running the TCKs to work around a problem there.
            // We do not document its use.
            useJaxRsSemantics = config
                    .get(USE_JAXRS_SEMANTICS_FULL_CONFIG_KEY)
                    .asBoolean()
                    .orElse(USE_JAXRS_SEMANTICS_DEFAULT);
            return this;
        }
    }
}
