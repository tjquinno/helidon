/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import io.smallrye.openapi.api.OpenApiConfig;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

class OpenAPIConfigTest {

    private final static String TEST_CONFIG_DIR = "src/test/resources";

    private static final JsonObject JSON_SCHEMA_OVERRIDE_DATE = Json.createObjectBuilder(Map.of())
            .add("name", "EpochMillis")
            .add("type", "number")
            .add("format", "int64")
            .add("description", "Milliseconds since January 1, 1970, 00:00:00 GMT")
            .build();

    private static final JsonObject JSON_SCHEMA_OVERRIDE_DURATION = Json.createObjectBuilder(Map.of())
            .add("name", "DurationNanoseconds")
            .add("type", "number")
            .add("format", "int64")
            .add("title", "Duration Nanoseconds")
            .add("description", "Number of nanoseconds represented by the duration")
            .build();

    private static final String SCHEMA_OVERRIDE_CONFIG_FQCN_DATE = "java.util.Date";
    private static final String SCHEMA_OVERRIDE_CONFIG_FQCN_DURATION = "java.time.Duration";

    // must escape dots in config keys
    private static final String SCHEMA_OVERRIDE_ESCAPED_CONFIG_KEY = Config.Key.escapeName(SCHEMA_OVERRIDE_CONFIG_FQCN_DURATION);

    @Test
    void simpleConfigTest() {
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .sources(ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "simple.properties").toString()))
                .build();
        HelidonOpenApiConfig helidonOpenAPIConfig = HelidonOpenApiConfig.builder()
                .config(config.get("openapi"))
                .build();

        assertThat("model reader", helidonOpenAPIConfig.modelReader(), is("io.helidon.openapi.test.MyModelReader"));
        assertThat("filter", helidonOpenAPIConfig.filter(), is("io.helidon.openapi.test.MySimpleFilter"));
        assertThat("servers", helidonOpenAPIConfig.servers(), containsInAnyOrder("s1", "s2"));
        assertThat("path1 servers", helidonOpenAPIConfig.pathServers("path1"), containsInAnyOrder("p1s1", "p1s2"));
        assertThat("path2 servers", helidonOpenAPIConfig.pathServers("path2"), containsInAnyOrder("p2s1", "p2s2"));
    }

    @Test
    void checkUnconfiguredValues() {
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .sources(ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "simple.properties").toString()))
                .build();
        HelidonOpenApiConfig helidonOpenAPIConfig = HelidonOpenApiConfig.builder()
                .config(config.get("openapi"))
                .build();

        OpenApiConfig openApiConfig = (HelidonOpenApiConfig.Builder.ConfigImpl) helidonOpenAPIConfig;
        assertThat("scan disable mismatch", openApiConfig.scanDisable(), is(true));
    }

    @Test
    void checkSchemaConfig() {
        Config config = Config.just(ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "simple.properties").toString()),
                                    ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "configWithJsonSchemaOverrides.yaml")
                                                               .toString()));
        HelidonOpenApiConfig helidonOpenAPIConfig = HelidonOpenApiConfig.builder()
                .config(config.get("openapi"))
                .build();

        assertThat("Schema override", helidonOpenAPIConfig.schemas(), hasKey(SCHEMA_OVERRIDE_CONFIG_FQCN_DATE));
        assertThat("Schema override value for " + SCHEMA_OVERRIDE_CONFIG_FQCN_DATE,
                   Json.createReader(new StringReader(helidonOpenAPIConfig
                                                              .schemas()
                                                              .get(SCHEMA_OVERRIDE_CONFIG_FQCN_DATE))).readObject(),
                   is(JSON_SCHEMA_OVERRIDE_DATE));

        assertThat("Schema override", helidonOpenAPIConfig.schemas(), hasKey(SCHEMA_OVERRIDE_CONFIG_FQCN_DURATION));
        assertThat("Schema override value for " + SCHEMA_OVERRIDE_ESCAPED_CONFIG_KEY,
                   Json.createReader(new StringReader(helidonOpenAPIConfig
                                                              .schemas()
                                                              .get(SCHEMA_OVERRIDE_CONFIG_FQCN_DURATION))).readObject(),
                   is(JSON_SCHEMA_OVERRIDE_DURATION));
    }

    @Test
    void checkSchemaConfigInProperties() {
        Config config = Config.just(ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "simple.properties")
                                                               .toString()),
                                    ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "configWithSchemaOverrides.properties")
                                                               .toString()));
        HelidonOpenApiConfig helidonOpenAPIConfig = HelidonOpenApiConfig.builder()
                .config(config.get("openapi"))
                .build();

        assertThat("Schema override", helidonOpenAPIConfig.schemas(), hasKey(SCHEMA_OVERRIDE_CONFIG_FQCN_DATE));
        assertThat("Schema override value for " + SCHEMA_OVERRIDE_CONFIG_FQCN_DATE,
                   Json.createReader(new StringReader(helidonOpenAPIConfig
                                                              .schemas()
                                                              .get(SCHEMA_OVERRIDE_CONFIG_FQCN_DATE))).readObject(),
                   is(JSON_SCHEMA_OVERRIDE_DATE));
    }

    @Test
    void testAdapter() {
        Config config = Config.just(ConfigSources.file(Paths.get(TEST_CONFIG_DIR, "configWithVariousSettings.yaml").toString()));
        HelidonOpenApiConfig helidonOpenApiConfig = HelidonOpenApiConfig.builder()
                .config(config.get("openapi"))
                .build();
        OpenApiConfig openApiConfig = (HelidonOpenApiConfig.Builder.ConfigImpl) helidonOpenApiConfig;

        assertThat("scan disable", openApiConfig.scanDisable(), is(true));
        assertThat("model reader", openApiConfig.modelReader(), is("io.helidon.openapi.MyTestReader"));
        assertThat("filter", openApiConfig.filter(), is("io.helidon.openapi.MyFilter"));
        assertThat("servers", openApiConfig.servers(), containsInAnyOrder("server1", "server2"));
        assertThat("array references enabled", openApiConfig.arrayReferencesEnable(), is(false));
        assertThat("application path disabled", openApiConfig.applicationPathDisable(), is(false));
        assertThat("private properties enabled", openApiConfig.privatePropertiesEnable(), is(false));
        assertThat("sorted properties enabled", openApiConfig.sortedPropertiesEnable(), is(true));
        assertThat("OpenAPI version", openApiConfig.getOpenApiVersion(), is("1.2.3.4"));
        assertThat("info title", openApiConfig.getInfoTitle(), is("My API"));
        assertThat("info version", openApiConfig.getInfoVersion(), is("1.0.0-SNAPSHOT"));
        assertThat("info description", openApiConfig.getInfoDescription(), is("My API for a REST service"));
        assertThat("info terms of service", openApiConfig.getInfoTermsOfService(), is("Generous"));
        assertThat("info conatct name", openApiConfig.getInfoContactName(), is("Joe Smith"));
        assertThat("info contact email", openApiConfig.getInfoContactEmail(), is("joe.smith@helidon.io"));
        assertThat("info contact url", openApiConfig.getInfoContactUrl(), is("https://helidon.io/people/Joe"));
        assertThat("info license name", openApiConfig.getInfoLicenseName(), is("Apache 2.0"));
        assertThat("info license url",
                   openApiConfig.getInfoLicenseUrl(),
                   is("https://www.apache.org/licenses/LICENSE-2.0"));
        assertThat("operation ID strategy",
                   openApiConfig.getOperationIdStrategy(),
                   is(OpenApiConfig.OperationIdStrategy.METHOD));
        assertThat("duplicate operation ID behavior",
                   openApiConfig.getDuplicateOperationIdBehavior(),
                   is(OpenApiConfig.DuplicateOperationIdBehavior.FAIL));
        assertThat("default produces",
                   openApiConfig.getDefaultProduces(),
                   optionalValue(arrayContaining("application/json")));
        assertThat("default consumes",
                   openApiConfig.getDefaultConsumes(),
                   optionalValue(arrayContaining("text/plain", "text/html")));
    }
}
