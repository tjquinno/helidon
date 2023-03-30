/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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


import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class TestAdditionalProperties {

    private static ParserHelper helper = SwaggerParserHelper.create();


    @Test
    void checkParsingBooleanAdditionalProperties() throws IOException {
        OpenAPI openAPI = ParserTest.parse(helper, "/withBooleanAddlProps.yml");
        Schema<?> itemSchema = openAPI.getComponents().getSchemas().get("item");

        Object additionalProperties = itemSchema.getAdditionalProperties();
        assertThat("Additional properties", additionalProperties, instanceOf(Boolean.class));
        assertThat("Additional properties value", (Boolean) additionalProperties, is(false));
    }

    @Test
    void checkParsingSchemaAdditionalProperties() throws IOException {
        OpenAPI openAPI = ParserTest.parse(helper, "/withSchemaAddlProps.yml");
        Schema<?> itemSchema = openAPI.getComponents().getSchemas().get("item");


        Object additionalProperties = itemSchema.getAdditionalProperties();
        assertThat("Additional properties", additionalProperties, instanceOf(Schema.class));

        Map<String, Schema> additionalPropertiesSettings = ((Schema<?>) additionalProperties).getProperties();
        assertThat("Additional property 'code'", additionalPropertiesSettings, hasKey("code"));
        assertThat("Additional property 'text'", additionalPropertiesSettings, hasKey("text"));
    }

    @Test
    void checkWritingSchemaAdditionalProperties() throws IOException {
        OpenAPI openAPI = ParserTest.parse(helper, "/withSchemaAddlProps.yml");
        String document = formatModel(openAPI);

        /*
         * Expected output (although the
               additionalProperties:
        type: object
        properties:
          code:
            type: integer
          text:
            type: string
         */
        Yaml yaml = new Yaml();
        Map<String, Object> model = yaml.load(document);
        Map<String, ?> item = asMap(model, "components", "schemas", "item");

        Object additionalProperties = item.get("additionalProperties");

        assertThat("Additional properties node type", additionalProperties, is(instanceOf(Map.class)));

    }

    private static Map<String, ?> asMap(Map<String, ?> map, String... keys) {
        Map<String, ?> m = map;
        for (String key : keys) {
            m = (Map<String, ?>) m.get(key);
        }
        return m;
    }

    @Test
    void checkWritingBooleanAdditionalProperties() throws IOException {
        OpenAPI openAPI = ParserTest.parse(helper, "/withBooleanAddlProps.yml");
        String document = formatModel(openAPI);

        /*
         * Expected output: additionalProperties: false
         */

        assertThat("Formatted OpenAPI document matches expected pattern",
                   document, containsString("additionalProperties: false"));
    }

    private String formatModel(OpenAPI model) {
        StringWriter sw = new StringWriter();
        Map<Class<?>, ExpandedTypeDescription> implsToTypes = ExpandedTypeDescription.buildImplsToTypes(helper);
        Serializer.serialize(helper.types(), implsToTypes, model, Format.YAML, sw);
        return sw.toString();
    }
}
