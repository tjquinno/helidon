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

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.microprofile.server.JaxRsApplication;
import io.helidon.microprofile.server.JaxRsCdiExtension;
import io.helidon.nima.openapi.OpenApiFeature;
import io.helidon.openapi.HelidonOpenApiConfig;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.ext.Provider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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

    /**
     * Builds a list of filtered index views, one for each JAX-RS application, sorted by the Application class name to help
     * keep the list of endpoints in the OpenAPI document in a stable order.
     * <p>
     * First, we find all resource, provider, and feature classes present in the index. This is the same for all
     * applications.
     * </p>
     * <p>
     * Each filtered index view is tuned to one JAX-RS application.
     * </p>
     *
     * @return list of {@code FilteredIndexView}s, one per JAX-RS application
     */
    private List<FilteredIndexView> buildPerAppFilteredIndexViews() {

        List<JaxRsApplication> jaxRsApplications = jaxRsApplicationsToRun().stream()
                .filter(jaxRsApp -> jaxRsApp.applicationClass().isPresent())
                .sorted(Comparator.comparing(jaxRsApplication -> jaxRsApplication.applicationClass()
                        .get()
                        .getName()))
                .collect(Collectors.toList());

        // TODO - fix this
        IndexView indexView = null; // = singleIndexViewSupplier.get();

        FilteredIndexView viewFilteredByConfig = new FilteredIndexView(indexView, helidonOpenApiConfig().openApiConfig());
        Set<String> ancillaryClassNames = ancillaryClassNames(viewFilteredByConfig);

        /*
         * Filter even for a single-application class in case it implements getClasses or getSingletons.
         */
        return jaxRsApplications.stream()
                .map(jaxRsApp -> indexViewHelper.filteredIndexView(viewFilteredByConfig,
                                                                   jaxRsApplications,
                                                                   jaxRsApp,
                                                                   ancillaryClassNames))
                .collect(Collectors.toList());
    }

    private static Set<String> ancillaryClassNames(IndexView indexView) {
        Set<String> result = new HashSet<>(resourceClassNames(indexView));
        result.addAll(providerClassNames(indexView));
        result.addAll(featureClassNames(indexView));
        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
            LOGGER.log(System.Logger.Level.TRACE, "Ancillary classes: {0}", result);
        }
        return result;
    }

    private static Set<String> resourceClassNames(IndexView indexView) {
        return annotatedClassNames(indexView, Path.class);
    }

    private static Set<String> providerClassNames(IndexView indexView) {
        return annotatedClassNames(indexView, Provider.class);
    }

    private static Set<String> featureClassNames(IndexView indexView) {
        return annotatedClassNames(indexView, Feature.class);
    }

    private static Set<String> annotatedClassNames(IndexView indexView, Class<?> annotationClass) {
        // Partially inspired by the SmallRye code.
        return indexView
                .getAnnotations(DotName.createSimple(annotationClass.getName()))
                .stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind() == AnnotationTarget.Kind.CLASS)
                .map(AnnotationTarget::asClass)
                .filter(classInfo -> hasImplementationOrIsIncluded(indexView, classInfo))
                .map(ClassInfo::toString)
                .collect(Collectors.toSet());
    }

    private static boolean hasImplementationOrIsIncluded(IndexView indexView, ClassInfo classInfo) {
        // Partially inspired by the SmallRye code.
        return !Modifier.isInterface(classInfo.flags())
                || indexView.getAllKnownImplementors(classInfo.name()).stream()
                .anyMatch(MpOpenApiFeature::isConcrete);
    }

    private static boolean isConcrete(ClassInfo classInfo) {
        return !Modifier.isAbstract(classInfo.flags());
    }

//    /**
//     * Builds a list of filtered index views, one for each JAX-RS application, sorted by the Application class name to help
//     * keep the list of endpoints in the OpenAPI document in a stable order.
//     * <p>
//     * First, we find all resource, provider, and feature classes present in the index. This is the same for all
//     * applications.
//     * </p>
//     * <p>
//     * Each filtered index view is tuned to one JAX-RS application.
//     *
//     * @return list of {@code FilteredIndexView}s, one per JAX-RS application
//     */
//    private List<FilteredIndexView> buildPerAppFilteredIndexViews() {
//
//        List<JaxRsApplication> jaxRsApplications = jaxRsApplicationsToRun().stream()
//                .filter(jaxRsApp -> jaxRsApp.applicationClass().isPresent())
//                .sorted(Comparator.comparing(jaxRsApplication -> jaxRsApplication.applicationClass()
//                        .get()
//                        .getName()))
//                .collect(Collectors.toList());
//
//        IndexView indexView = singleIndexViewSupplier.get();
//
//        FilteredIndexView viewFilteredByConfig = new FilteredIndexView(indexView, OpenApiConfigImpl.fromConfig(mpConfig));
//        Set<String> ancillaryClassNames = ancillaryClassNames(viewFilteredByConfig);
//
//        /*
//         * Filter even for a single-application class in case it implements getClasses or getSingletons.
//         */
//        return jaxRsApplications.stream()
//                .map(jaxRsApp -> filteredIndexView(viewFilteredByConfig,
//                                                   jaxRsApplications,
//                                                   jaxRsApp,
//                                                   ancillaryClassNames))
//                .collect(Collectors.toList());
//    }
//
//    private static String toClassName(JaxRsApplication jaxRsApplication) {
//        return jaxRsApplication.applicationClass()
//                .map(Class::getName)
//                .orElse("<unknown>");
//    }
//
//    private static Set<String> classNamesToIgnore(List<JaxRsApplication> jaxRsApplications,
//                                                  JaxRsApplication jaxRsApp,
//                                                  Set<String> ancillaryClassNames,
//                                                  Set<String> classesExplicitlyReferenced) {
//
//        String appClassName = toClassName(jaxRsApp);
//
//        Set<String> result = // Start with all other JAX-RS app names.
//                jaxRsApplications.stream()
//                        .map(Builder::toClassName)
//                        .filter(candidateName -> !candidateName.equals("<unknown>") && !candidateName.equals(appClassName))
//                        .collect(Collectors.toSet());
//
//        if (!classesExplicitlyReferenced.isEmpty()) {
//            // This class identified resource, provider, or feature classes it uses. Ignore all ancillary classes that this app
//            // does not explicitly reference.
//            result.addAll(ancillaryClassNames);
//            result.removeAll(classesExplicitlyReferenced);
//        }
//
//        return result;
//    }
//
//    private static Set<String> ancillaryClassNames(IndexView indexView) {
//        Set<String> result = new HashSet<>(resourceClassNames(indexView));
//        result.addAll(providerClassNames(indexView));
//        result.addAll(featureClassNames(indexView));
//        if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
//            LOGGER.log(System.Logger.Level.TRACE, "Ancillary classes: {0}", result);
//        }
//        return result;
//    }
//
//    private static Set<String> resourceClassNames(IndexView indexView) {
//        return annotatedClassNames(indexView, Path.class);
//    }
//
//    private static Set<String> providerClassNames(IndexView indexView) {
//        return annotatedClassNames(indexView, Provider.class);
//    }
//
//    private static Set<String> featureClassNames(IndexView indexView) {
//        return annotatedClassNames(indexView, Feature.class);
//    }
//
//    private static Set<String> annotatedClassNames(IndexView indexView, Class<?> annotationClass) {
//        // Partially inspired by the SmallRye code.
//        return indexView
//                .getAnnotations(DotName.createSimple(annotationClass.getName()))
//                .stream()
//                .map(AnnotationInstance::target)
//                .filter(target -> target.kind() == AnnotationTarget.Kind.CLASS)
//                .map(AnnotationTarget::asClass)
//                .filter(classInfo -> hasImplementationOrIsIncluded(indexView, classInfo))
//                .map(ClassInfo::toString)
//                .collect(Collectors.toSet());
//    }
//
//    private static boolean hasImplementationOrIsIncluded(IndexView indexView, ClassInfo classInfo) {
//        // Partially inspired by the SmallRye code.
//        return !Modifier.isInterface(classInfo.flags())
//                || indexView.getAllKnownImplementors(classInfo.name()).stream()
//                .anyMatch(Builder::isConcrete);
//    }
//
//    private static boolean isConcrete(ClassInfo classInfo) {
//        return !Modifier.isAbstract(classInfo.flags());
//    }


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
        @ConfiguredOption(type = OpenApiFeature.class, mergeWithParent = true)
        public MpOpenApiFeature build() {
            // We need to defer some work until later when the server has started the applications.
            return new MpOpenApiFeature(this);
        }

        @Override
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
        @ConfiguredOption(type = OpenApiConfig.class, mergeWithParent = true)
        @ConfiguredOption(key = "scan.disable",
                          type = Boolean.class,
                          value = "false",
                          description = "Disable annotation scanning.")
        @ConfiguredOption(key = "scan.packages",
                          type = String.class,
                          kind = ConfiguredOption.Kind.LIST,
                          description = "Specify the list of packages to scan.")
        @ConfiguredOption(key = "scan.classes",
                          type = String.class,
                          kind = ConfiguredOption.Kind.LIST,
                          description = "Specify the list of classes to scan.")
        @ConfiguredOption(key = "scan.exclude.packages",
                          type = String.class,
                          kind = ConfiguredOption.Kind.LIST,
                          description = "Specify the list of packages to exclude from scans.")
        @ConfiguredOption(key = "scan.exclude.classes",
                          type = String.class,
                          kind = ConfiguredOption.Kind.LIST,
                          description = "Specify the list of classes to exclude from scans.")
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
