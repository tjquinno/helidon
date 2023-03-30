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
package io.helidon.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ParserTest {

    private static ParserHelper helper = SwaggerParserHelper.create();

    @Test
    public void testParserUsingYAML() throws IOException {
        OpenAPI openAPI = parse(helper, "/petstore.yaml");
        assertThat(openAPI.getOpenapi(), is("3.0.0"));
        assertThat(openAPI.getPaths().get("/pets").getGet().getParameters().get(0).getIn(),
                is("query"));
    }

    @Test
    public void testExtensions() throws IOException {
        OpenAPI openAPI = parse(helper, "/openapi-greeting.yml");
        Object xMyPersonalMap = openAPI.getExtensions().get("x-my-personal-map");
        assertThat(xMyPersonalMap, is(instanceOf(Map.class)));
        Map<?,?> map = (Map<?,?>) xMyPersonalMap;
        Object owner = map.get("owner");
        Object value1 = map.get("value-1");
        assertThat(value1, is(instanceOf(Double.class)));
        Double d = (Double) value1;
        assertThat(d, equalTo(2.3));

        assertThat(owner, is(instanceOf(Map.class)));
        map = (Map<?,?>) owner;
        assertThat(map.get("first"), equalTo("Me"));
        assertThat(map.get("last"), equalTo("Myself"));

        Object xBoolean = openAPI.getExtensions().get("x-boolean");
        assertThat(xBoolean, is(instanceOf(Boolean.class)));
        Boolean b = (Boolean) xBoolean;
        assertThat(b, is(true));

        Object xInt = openAPI.getExtensions().get("x-int");
        assertThat(xInt, is(instanceOf(Integer.class)));
        Integer i = (Integer) xInt;
        assertThat(i, is(117));

        Object xStrings = openAPI.getExtensions().get("x-string-array");
        assertThat(xStrings, is(instanceOf(List.class)));
        List<?> list = (List<?>) xStrings;
        Object first = list.get(0);
        assertThat(first, is(instanceOf(String.class)));
        String f = (String) first;
        assertThat(f, is(equalTo("one")));
    }


    @Test
    void testYamlRef() throws IOException {
        OpenAPI openAPI = parse(helper, "/petstore.yaml");
        Paths paths = openAPI.getPaths();
        String ref = paths.get("/pets")
                .getGet()
                .getResponses()
                .get("200")
                .getContent()
                .get("application/json")
                .getSchema()
                .get$ref();

        assertThat("ref value", ref, is(equalTo("#/components/schemas/Pets")));
    }

    @Test
    void testJsonRef() throws IOException {
        OpenAPI openAPI = parse(helper, "/petstore.json");
        Paths paths = openAPI.getPaths();
        String ref = paths.get("/user")
                .getPost()
                .getRequestBody()
                .getContent()
                .get("application/json")
                .getSchema()
                .get$ref();

                assertThat("ref value", ref, is(equalTo("#/components/schemas/User")));
    }

    @Test
    public void testParserUsingJSON() throws IOException {
        OpenAPI openAPI = parse(helper, "/petstore.json");
        assertThat(openAPI.getOpenapi(), is("3.0.0"));
// TODO - uncomment the following once full $ref support is in place
//        assertThat(openAPI.getPaths().getPathItem("/pet").getPUT().getRequestBody().getDescription(),
//                containsString("needs to be added to the store"));
    }

    static OpenAPI parse(ParserHelper helper, String path) throws IOException {
        try (InputStream is = ParserTest.class.getResourceAsStream(path)) {
            return OpenAPIParser.parse(helper.types(), is);
        }
    }
}