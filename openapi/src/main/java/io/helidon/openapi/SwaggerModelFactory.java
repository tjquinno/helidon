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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Implemention of {@link io.helidon.openapi.ModelFactory} for the Swagger model library.
 * <p>
 *     Extensible classes in the Swagger library do not extend or implement a common class or interface
 *     for extensibility; they share a pattern of method names. For a little bit of performance gain we cache
 *     the {@code Method} instances related to each extensible Swagger model class that is extensible.
 * </p>
 */
class SwaggerModelFactory implements ModelFactory {

    record ExtensibilityMethods(Method getExtensions, Method addExtension, Method setExtensions, Method extensions) {

        static ExtensibilityMethods create(Class<?> type) {
            try {
                return new ExtensibilityMethods(type.getMethod("getExtensions"),
                                                type.getMethod("addExtension", String.class, Object.class),
                                                type.getMethod("setExtensions", Map.class),
                                                type.getMethod("extensions", Map.class));
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    record ReferenceabilityMethods(Method getRef, Method setRef, Method ref) {

        static ReferenceabilityMethods create(Class<?> type) {
            try {
                return new ReferenceabilityMethods(type.getMethod("get$ref"),
                                                   type.getMethod("set$ref", String.class),
                                                   type.getMethod("$ref", String.class));
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    private final Map<Class<?>, ExtensibilityMethods> extensibilityMethods = new HashMap<>();

    private final Map<Class<?>, ReferenceabilityMethods> referenceabilityMethods = new HashMap<>();

    @Override
    public void additionalProperties(Object untypedSchema, Object value) {
        assert untypedSchema instanceof Schema;
        ((Schema<?>) untypedSchema).setAdditionalProperties(value);
    }

    @Override
    public Object additionalProperties(Object untypedSchema) {
        assert untypedSchema instanceof Schema;
        return ((Schema<?>) untypedSchema).getAdditionalProperties();
    }

    @Override
    public Method placeholderGetAdditionalProperties() {
        try {
            return Schema.class.getMethod("getAdditionalProperties");
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Method placeholderSetAdditionalProperties() {
        try {
            return Schema.class.getMethod("setAdditionalProperties", Object.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public Class<?> schemaType() {
        return Schema.class;
    }

    @Override
    public Class<?> apiResponsesType() {
        return ApiResponses.class;
    }

    @Override
    public boolean isSchema(Class<?> typeClass) {
        return Schema.class.isAssignableFrom(typeClass);
    }

    @Override
    public boolean isExtensible(Class<?> typeClass) {
        return SwaggerParserHelper.isExtensible(typeClass);
    }

    @Override
    public boolean isReferenceable(Object object) {
        try {
            referenceability(object);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void addExtension(Object extensible, String name, Object value) {
        try {
            extensibility(extensible).addExtension.invoke(extensible, name, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getExtensions(Object extensible) {
        try {
            return (Map<String, Object>) extensibility(extensible).getExtensions.invoke(extensible);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setExtensions(Object extensible, Map<String, Object> extensions) {
        try {
            extensibility(extensible).setExtensions.invoke(extensible, extensions);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRefMethodName() {
        return "get$ref";
    }

    @Override
    public String setRefMethodName() {
        return "set$ref";
    }

    @Override
    public String refFieldName() {
        return "$ref";
    }

    private ExtensibilityMethods extensibility(Object extensible) {
        return extensibilityMethods.computeIfAbsent(extensible.getClass(), ExtensibilityMethods::create);
    }

    private ReferenceabilityMethods referenceability(Object referenceable) {
        return referenceabilityMethods.computeIfAbsent(referenceable.getClass(), ReferenceabilityMethods::create);
    }

}
