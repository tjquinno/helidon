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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import io.helidon.config.Config;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;

/**
 * Utility methods for converting config nodes to JSON elements.
 */
class JsonConverter {

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());
    private static final JsonWriterFactory WRITER_FACTORY = Json.createWriterFactory(Map.of());

    private JsonConverter() {
    }

    static void addJsonValue(JsonObjectBuilder jsonBuilder, Config config) {
        String name = config.name();
        String configValue = config.asString().get();
        try {

            jsonBuilder.add(name, Integer.parseInt(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Intentionally fall through and try the next.
        }
        try {
            jsonBuilder.add(name, Long.parseLong(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Intentionally fall through
        }
        try {
            jsonBuilder.add(name, Double.parseDouble(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Intentionally fall through.
        }
        if (configValue.equals("true") || configValue.equals("false")) {
            jsonBuilder.add(name, Boolean.parseBoolean(configValue));
            return;
        }
        jsonBuilder.add(name, configValue);
    }

    static void addJsonValue(JsonArrayBuilder arrayBuilder, Config config) {
        String configValue = config.asString().get();

        try {
            arrayBuilder.add(Integer.parseInt(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Fall through
        }
        try {
            arrayBuilder.add(Long.parseLong(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Fall through
        }
        try {
            arrayBuilder.add(Double.parseDouble(configValue));
            return;
        } catch (NumberFormatException ex) {
            // Fall through
        }
        if (configValue.equalsIgnoreCase("true")) {
            arrayBuilder.add(JsonValue.TRUE);
            return;
        } else if (configValue.equalsIgnoreCase("false")) {
            arrayBuilder.add(JsonValue.FALSE);
            return;
        }
        arrayBuilder.add(configValue);
    }

    static JsonObjectBuilder asObject(Config node) {
        JsonObjectBuilder builder = JSON.createObjectBuilder(Map.of());
        node.asNodeList()
                .get()
                .forEach(subNode -> {
                        switch (subNode.type()) {
                        case VALUE -> addJsonValue(builder, subNode);
                        case OBJECT -> builder.add(subNode.name(), asObject(subNode));
                        case LIST -> builder.add(subNode.name(), asArray(subNode));
                        default -> {}
                        }
        });
        return builder;
    }

    static JsonArrayBuilder asArray(Config node) {
        JsonArrayBuilder builder = JSON.createArrayBuilder();
        node.asNodeList()
                .get()
                .forEach(subNode -> {
                    switch (subNode.type()) {
                    case VALUE -> addJsonValue(builder, subNode);
                    case OBJECT -> builder.add(asObject(subNode));
                    case LIST -> builder.add(asArray(subNode));
                    default -> {}
                    }
                });
        return builder;
    }

    static String toJsonText(Config config) {
        return toJsonText(asObject(config).build());
    }

    static String toJsonText(JsonValue jsonValue) {
        StringWriter sw = new StringWriter();
        JsonWriter writer = WRITER_FACTORY.createWriter(sw);
        writer.write(jsonValue);
        writer.close();
        return sw.toString();
    }

    static JsonObject asObject(String jsonText) {
        return Json.createParser(new StringReader(jsonText)).getObject();
    }
}
