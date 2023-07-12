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
package io.helidon.metrics;

import java.util.Optional;

import io.helidon.common.testing.junit5.OptionalMatcher;
import io.helidon.metrics.api.MetricsProgrammaticSettings;
import io.helidon.metrics.api.Registry;
import io.helidon.metrics.api.RegistryFactory;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class TestJsonFormatter {

    private static JsonFormatter formatter;
    private static Registry appRegistry = RegistryFactory.getInstance().getRegistry(Registry.APPLICATION_SCOPE);
    private static Registry myRegistry = RegistryFactory.getInstance().getRegistry("jsonFormatterTestScope");

    private static final String JSON_COUNTER_1_NAME = "counter1";
    private static final String JSON_COUNTER_1_DESC = "This is counter 1";
    private static final String JSON_COUNTER_2_NAME = "counter2";
    private static final String JSON_COUNTER_2_DESC = "This is counter 2";

    private static Metadata counter1Metadata = Metadata.builder()
            .withName(JSON_COUNTER_1_NAME)
            .withDescription(JSON_COUNTER_1_DESC)
            .build();

    private static Metadata counter2Metadata = Metadata.builder()
            .withName(JSON_COUNTER_2_NAME)
            .withDescription(JSON_COUNTER_2_DESC)
            .build();

    private static Counter counter1;
    private static Counter counter2;
    private static Counter counter2a;

    @BeforeAll
    static void init() {
        formatter = JsonFormatter.builder()
                .scopeTagName(MetricsProgrammaticSettings.instance().scopeTagName())
                .build();
        counter1 = appRegistry.counter(counter1Metadata);
        counter1.inc(2L);
        counter2 = myRegistry.counter(counter2Metadata,
                                      new Tag("t1", "1"),
                                      new Tag("t2", "2"));
        counter2.inc(3L);
        counter2a = myRegistry.counter(counter2Metadata,
                                       new Tag("t1", "1a"),
                                       new Tag("t2", "2a"));
    }
    @Test
    void testCounter() {

        Optional<JsonObject> result = formatter.data(false);

        assertThat("Result", result, OptionalMatcher.optionalPresent());
        assertThat("Counter 1",
                   result.get().getJsonNumber(JSON_COUNTER_1_NAME).intValue(),
                   is(2));
        assertThat("Counter 2",
                   result.get().getJsonNumber(JSON_COUNTER_2_NAME + ";t1=1;t2=2").intValue(),
                   is(3));
    }

    @Test
    void testMetadataWithinScope() {
        Optional<JsonObject> result = formatter.metadata(false);

        assertThat("Result", result, OptionalMatcher.optionalPresent());
        JsonObject counter1Metadata = result.get().getJsonObject(JSON_COUNTER_1_NAME);
        assertThat("Counter 1 metadata", counter1Metadata, is(notNullValue()));
        assertThat("Counter 1 desc", counter1Metadata.getString("description"), is(JSON_COUNTER_1_DESC));

        JsonObject counter2Metadata = result.get().getJsonObject(JSON_COUNTER_2_NAME);
        assertThat("Counter 2 metadata", counter2Metadata, is(notNullValue()));
        assertThat("Counter 2 desc", counter2Metadata.getString("description"), is(JSON_COUNTER_2_DESC));
        JsonArray tagsForCounter2 = counter2Metadata.getJsonArray("tags");
        assertThat("Tags for Counter 2", tagsForCounter2, is(notNullValue()));
        JsonArray firstTagSetForCounter2 = tagsForCounter2.getJsonArray(0);
        assertThat("First tag set for Counter 2", firstTagSetForCounter2, is(notNullValue()));
        assertThat("First tag in first set for Counter 2", firstTagSetForCounter2.getString(0), is(equalTo("t1=1")));
        assertThat("Second tag in first set for Counter 2", firstTagSetForCounter2.getString(1), is(equalTo("t2=2")));
        JsonArray secondTagSetForCounter2 = tagsForCounter2.getJsonArray(1);
        assertThat("Second tag set for Counter 2", secondTagSetForCounter2, is(notNullValue()));
        assertThat("First tag in second set for Counter 2", secondTagSetForCounter2.getString(0), is(equalTo("t1=1a")));
        assertThat("Second tag in second set for Counter 2", secondTagSetForCounter2.getString(1), is(equalTo("t2=2a")));
    }

    @Test
    void testMetadataAcrossScopes() {
        Optional<JsonObject> result = formatter.metadata(true);

        assertThat("Result", result, OptionalMatcher.optionalPresent());
        JsonObject app = result.get().getJsonObject(Registry.APPLICATION_SCOPE);
        assertThat("App node", app, is(notNullValue()));
        JsonObject counter1Metadata = app.getJsonObject(JSON_COUNTER_1_NAME);
        assertThat("Counter 1 metadata", counter1Metadata, is(notNullValue()));
        assertThat("Counter 1 desc", counter1Metadata.getString("description"), is(JSON_COUNTER_1_DESC));


        JsonObject custom = result.get().getJsonObject(myRegistry.scope());
        assertThat("Custom scope", custom, is(notNullValue()));
        JsonObject counter2Metadata = custom.getJsonObject(JSON_COUNTER_2_NAME);
        assertThat("Counter 2 metadata", counter2Metadata, is(notNullValue()));
        assertThat("Counter 2 desc", counter2Metadata.getString("description"), is(JSON_COUNTER_2_DESC));

        assertThat("Counter 2 metadata", counter2Metadata, is(notNullValue()));
        assertThat("Counter 2 desc", counter2Metadata.getString("description"), is(JSON_COUNTER_2_DESC));
        JsonArray tagsForCounter2 = counter2Metadata.getJsonArray("tags");
        assertThat("Tags for Counter 2", tagsForCounter2, is(notNullValue()));
        JsonArray firstTagSetForCounter2 = tagsForCounter2.getJsonArray(0);
        assertThat("First tag set for Counter 2", firstTagSetForCounter2, is(notNullValue()));
        assertThat("First tag in first set for Counter 2", firstTagSetForCounter2.getString(0), is(equalTo("t1=1")));
        assertThat("Second tag in first set for Counter 2", firstTagSetForCounter2.getString(1), is(equalTo("t2=2")));
        JsonArray secondTagSetForCounter2 = tagsForCounter2.getJsonArray(1);
        assertThat("Second tag set for Counter 2", secondTagSetForCounter2, is(notNullValue()));
        assertThat("First tag in second set for Counter 2", secondTagSetForCounter2.getString(0), is(equalTo("t1=1a")));
        assertThat("Second tag in second set for Counter 2", secondTagSetForCounter2.getString(1), is(equalTo("t2=2a")));

    }
}
