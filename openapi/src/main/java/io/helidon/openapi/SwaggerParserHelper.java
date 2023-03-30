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

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.ServerVariable;

/**
 * {@link ParserHelper} implementation for the Swagger model library.
 */
public class SwaggerParserHelper extends ParserHelper {

    /**
     * Create a new parser helper.
     *
     * @return a new parser helper
     */
    public static SwaggerParserHelper create() {
        return new SwaggerParserHelper(SnakeYAMLParserHelper.create(ExpandedTypeDescription::create));
    }

    static boolean isExtensible(Class<?> typeClass) {
        try {
            SwaggerModelFactory.ExtensibilityMethods.create(typeClass);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SwaggerParserHelper(SnakeYAMLParserHelper<ExpandedTypeDescription> generatedHelper) {
        super(generatedHelper);
    }

    @Override
    protected void adjustTypeDescriptions(Map<Class<?>, ExpandedTypeDescription> types) {
        /*
         * We need to adjust the {@code TypeDescription} objects set up by the generated {@code SnakeYAMLParserHelper} class
         * because there are some OpenAPI-specific issues that the general-purpose helper generator cannot know about.
         */

        /*
         * In the OpenAPI document, HTTP methods are expressed in lower-case. But the associated Java methods on the PathItem
         * class use the HTTP method names in upper-case. So for each HTTP method, "add" a property to PathItem's type
         * description using the lower-case name but upper-case Java methods and exclude the upper-case property that
         * SnakeYAML's automatic analysis of the class already created.
         */
        ExpandedTypeDescription pathItemTD = types.get(PathItem.class);
        for (PathItem.HttpMethod m : PathItem.HttpMethod.values()) {
            pathItemTD.substituteProperty(m.name().toLowerCase(), Operation.class, getter(m), setter(m));
            pathItemTD.addExcludes(m.name());
        }

        /*
         * An OpenAPI document can contain a property named "enum" for Schema and ServerVariable, but the related Java methods
         * use "enumeration".
         */
        Set.<Class<?>>of(Schema.class, ServerVariable.class).forEach(c -> {
            ExpandedTypeDescription tdWithEnumeration = types.get(c);
            tdWithEnumeration.substituteProperty("enum", List.class, "getEnumeration", "setEnumeration");
            tdWithEnumeration.addPropertyParameters("enum", String.class);
            tdWithEnumeration.addExcludes("enumeration");
        });

        /*
         * SnakeYAML derives properties only from methods declared directly by each OpenAPI interface, not from methods defined
         *  on other interfaces which the original one extends. Those we have to handle explicitly.
         */
        for (ExpandedTypeDescription td : types.values()) {
            if (isExtensible(td.getType())) {
                td.addExtensions();
            }
            if (td.hasDefaultProperty()) {
                td.substituteProperty("default", Object.class, "getDefaultValue", "setDefaultValue");
                td.addExcludes("defaultValue");
            }
            if (td.isRef()) {
                td.addRef();
            }
        }
    }

    private static String getter(PathItem.HttpMethod method) {
        return methodName("get", method);
    }

    private static String setter(PathItem.HttpMethod method) {
        return methodName("set", method);
    }

    private static String methodName(String operation, PathItem.HttpMethod method) {
        return operation + method.name();
    }
}
