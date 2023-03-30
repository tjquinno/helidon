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

import java.lang.reflect.Method;
import java.util.Map;

import org.yaml.snakeyaml.introspector.Property;

/**
 * Behavior for creating or reasoning about OpenAPI model classes.
 * <p>
 *     We use Swagger model classes for non-MP code but MicroProfile OpenAPI model classes for MP code.
 *     This interface and its implementations insulate most of our OpenAPI code from the difference
 *     between the Swagger and MP OpenAPI object models.
 * </p>
 */
public interface ModelFactory {

    /**
     * Sets the additional properties value on a schema instance.
     *
     * @param untypedSchema the schema
     * @param value the additional properties value
     */
    void additionalProperties(Object untypedSchema, Object value);

    /**
     * Returns the additional properties value from a schema instance.
     *
     * @param untypedSchema the schema
     * @return the additional properties value
     */
    Object additionalProperties(Object untypedSchema);

    /**
     * Returns a {@code Method} for getting the {@code additionalProperties} from a schema.
     * The returned method is used only as a placeholder but needs to be a non-null, valid method.
     *
     * @return Method for a placeholder method
     */
    Method placeholderGetAdditionalProperties();

    /**
     * Returns a {@code Method} for setting the {@code additionalProperties} on a schema.
     * The returned method is used only as a placeholder but needs to be a non-null, valid method.
     *
     * @return Method for a placeholder method
     */
    Method placeholderSetAdditionalProperties();

    /**
     * Returns the type of the model-specific API responses interface or class.
     *
     * @return type of the API responses element
     */
    Class<?> apiResponsesType();

    /**
     * Returns the type of the model-specific {@code Schema}.
     *
     * @return type of the {@code Schema} type in the model
     */
    Class<?> schemaType();

    /**
     * Returns whether the type is extensible.
     *
     * @param typeClass the type to check
     * @return true/false
     */
    boolean isExtensible(Class<?> typeClass);

    /**
     * Returns whether the specified type is a {@code Schema} in the model.
     *
     * @param typeClass class to check
     * @return true/false
     */
    default boolean isSchema(Class<?> typeClass) {
        return schemaType().isAssignableFrom(typeClass);
    }

    /**
     * Returns whether the specified object is a {@code Schema}.
     *
     * @param object the object to check
     * @return true/false
     */
    default boolean isSchema(Object object) {
        return isSchema(object.getClass());
    }

    /**
     * Returns wehter the specified object can be a reference.
     *
     * @param object the object to check
     * @return true/false
     */
    boolean isReferenceable(Object object);

    /**
     * Returns whether the specified property on the specified object should be processed as a visible
     * property in serializing the object model to the OpenAPI document.
     *
     * @param object the object to check
     * @param property the name of the property
     * @return true/false
     */
    default boolean isOkToProcess(Object object, Property property) {
        return true;
    }

    /**
     * Adds an extension name and value pair to an extensible object.
     *
     * @param extensible the extensible object
     * @param name name of the extension property
     * @param value value of the extension property
     */
    void addExtension(Object extensible, String name, Object value);

    /**
     * Returns the extension properties set on the extensible object.
     *
     * @param extensible the extensible object
     * @return {@code Map} of the extension property names/values
     */
    Map<String, Object> getExtensions(Object extensible);

    /**
     * Sets the extension properties on the specified extensible object.
     *
     * @param extensible the extensible object
     * @param extensions the extension properties to set
     * @return the updated extensible object
     * @param <T> type of the extensible object
     */
    default <T> T extensions(Object extensible, Map<String, Object> extensions) {
        setExtensions(extensible, extensions);
        T t = (T) this;
        return t;
    }

    /**
     * Sets the extension properties on the specific extensible object.
     *
     * @param extensible the extensible object
     * @param extensions the extension property names/values
     */
    void setExtensions(Object extensible, Map<String, Object> extensions);

    /**
     * Returns the method name for getting a ref from a model object.
     *
     * @return the "get reference" method name
     */
    String getRefMethodName();

    /**
     * Returns the method name for setting a ref on a model object.
     *
     * @return the "set reference" method name
     */
    String setRefMethodName();

    /**
     * Returns the name of the field holding the reference.
     *
     * @return the name of the reference field
     */
    String refFieldName();
}
