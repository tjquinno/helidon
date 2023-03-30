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

import java.lang.reflect.Method;
import java.util.Map;

import io.helidon.openapi.ModelFactory;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.yaml.snakeyaml.introspector.Property;

public class MpModelFactory implements ModelFactory {

    @Override
    public void additionalProperties(Object untypedSchema, Object value) {
        assert untypedSchema instanceof Schema;
        if (value instanceof Boolean b) {
            ((Schema) untypedSchema).setAdditionalPropertiesBoolean(b);
        } else if (value instanceof Schema s) {
            ((Schema) untypedSchema).setAdditionalPropertiesSchema(s);
        } else {
            throw new IllegalArgumentException("Expected Boolean or Schema but received " + value.getClass().getName());
        }
    }

    @Override
    public Object additionalProperties(Object untypedSchema) {
        assert untypedSchema instanceof Schema;
        Object result = ((Schema) untypedSchema).getAdditionalPropertiesBoolean();
        if (result == null) {
            result = ((Schema) untypedSchema).getAdditionalPropertiesSchema();
        }
        return result;
    }

    @Override
    public Method placeholderGetAdditionalProperties() {
        try {
            return Schema.class.getMethod("getAdditionalPropertiesBoolean");
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public Method placeholderSetAdditionalProperties() {
        try {
            return Schema.class.getMethod("setAdditionalPropertiesBoolean", Boolean.class);
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
        return APIResponses.class;
    }

    @Override
    public boolean isSchema(Class<?> typeClass) {
        return Schema.class.isAssignableFrom(typeClass);
    }

    @Override
    public boolean isExtensible(Class<?> typeClass) {
        return Extensible.class.isAssignableFrom(typeClass);
    }

    @Override
    public boolean isReferenceable(Object object) {
        for (Class<?> c : object.getClass().getInterfaces()) {
            if (c.equals(Reference.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isOkToProcess(Object object, Property property) {
        /*
         * The following construct might look awkward - and it is. But if SmallRye adds additional properties to its
         * implementation classes that are not in the corresponding interfaces - and therefore we want to skip processing
         * them - then we can just add additional lines like the "reject |= ..." one, testing for the new case, without
         * having to change any other lines in the method.
         */
        boolean reject = false;
        reject |= Parameter.class.isAssignableFrom(object.getClass()) && property.getName().equals("hidden");
        return !reject;
    }

    @Override
    public void addExtension(Object extensible, String name, Object value) {
        assert extensible instanceof Extensible<?>;
        ((Extensible<?>) extensible).addExtension(name, value);
    }

    @Override
    public Map<String, Object> getExtensions(Object extensible) {
        assert extensible instanceof Extensible<?>;
        return ((Extensible<?>) extensible).getExtensions();
    }

    @Override
    public void setExtensions(Object extensible, Map<String, Object> extensions) {
        assert extensible instanceof Extensible<?>;
        ((Extensible<?>) extensible).setExtensions(extensions);
    }

    @Override
    public String getRefMethodName() {
        return "getRef";
    }

    @Override
    public String setRefMethodName() {
        return "setRef";
    }

    @Override
    public String refFieldName() {
        return "$ref";
    }
}
