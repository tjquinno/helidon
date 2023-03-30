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

import io.helidon.openapi.ExpandedTypeDescription;

import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

class MpExpandedTypeDescription extends ExpandedTypeDescription {


    public static MpExpandedTypeDescription create(Class<?> typeClass, Class<?> implemenetationClass) {
        return new MpExpandedTypeDescription(typeClass, implemenetationClass);
    }

    private MpExpandedTypeDescription(Class<?> typeClass, Class<?> implemenetationClass) {
        super(typeClass, implemenetationClass);
    }

    static class MpSchemaTypeDescription extends ExpandedTypeDescription.SchemaTypeDescription {

        MpSchemaTypeDescription(Class<?> typeClass, Class<?> implementationClass) {
            super(typeClass, implementationClass);
        }

        @Override
        protected void additionalProperties(Object untypedSchema, Object value) {
            assert untypedSchema instanceof Schema;
            Schema schema = (Schema) untypedSchema;
            if (value instanceof Schema) {
                schema.setAdditionalPropertiesSchema((Schema) value);
            } else {
                assert value instanceof Boolean;
                schema.setAdditionalPropertiesBoolean((Boolean) value);
            }
        }

        @Override
        protected Class<?> additionalPropertiesType(Node valueNode) {
            return valueNode.getTag().equals(Tag.BOOL) ? Boolean.class : Schema.class;
        }

        @Override
        protected Method getAdditionalPropertiesPlaceholderMethod() throws NoSuchMethodException {
            return Schema.class.getMethod("getAdditionalPropertiesSchema");
        }
    }


    @Override
    protected boolean isRef() {
        return Reference.class.isAssignableFrom(getType());
    }
}
