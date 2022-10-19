/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

package io.helidon.common.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Map}-based {@link Parameters} implementation with keys and immutable {@link List} of values that needs to be copied
 * on each write. By default, this implementation uses case-sensitive keys and a {@code ConcurrentSkipListMap} but
 * a subclass can furnish its own map factory to control what specific type of map is used.
 */
public class HashParameters extends NamedValueListsImpl<Parameters> implements Parameters {

    private static final List<String> EMPTY_STRING_LIST = Collections.emptyList();

    /**
     * Creates a new instance.
     */
    protected HashParameters() {
        super();
    }

    /**
     * Creates a new instance from provided data.
     * Initial data are copied.
     *
     * @param initialContent initial content.
     */
    protected HashParameters(Map<String, List<String>> initialContent) {
        super(initialContent);
    }

    /**
     * Creates a new instance from provided data, typically either another {@code Parameters} instance or a map's entry set.
     * Initial data are copied.
     *
     * @param initialContent initial content
     */
    protected HashParameters(Iterable<Map.Entry<String, List<String>>> initialContent) {
        super(initialContent);
    }

    /**
     * Creates a new instance from provided data.
     * Initial data is copied.
     *
     * @param initialContent initial content.
     */
    protected HashParameters(Parameters initialContent) {
        super(initialContent);
    }

    /**
     * Creates a new empty instance {@link HashParameters}.
     *
     * @return a new instance of {@link HashParameters}.
     */
    public static HashParameters create() {
        return new HashParameters();
    }

    /**
     * Creates a new instance of {@link HashParameters} from a single provided Parameter or Map. Initial data is copied.
     *
     * @param initialContent initial content.
     * @return a new instance of {@link HashParameters} initialized with the given content.
     */
    public static HashParameters create(Map<String, List<String>> initialContent) {
        return new HashParameters(initialContent == null ? Collections.emptySet() : initialContent.entrySet());
    }

    /**
     * Creates a new instance {@link HashParameters} from provided data. Initial data is copied.
     *
     * @param initialContent initial content.
     * @return a new instance of {@link HashParameters} initialized with the given content.
     */
    public static HashParameters create(Parameters initialContent) {
        return new HashParameters(initialContent);
    }

    /**
     * Creates a new instance of {@link HashParameters} from a single provided Parameter or Map. Initial data is copied.
     *
     * @param initialContent initial content.
     * @return a new instance of {@link HashParameters} initialized with the given content.
     */
    public static HashParameters create(Iterable<Map.Entry<String, List<String>>> initialContent) {
        return new HashParameters(initialContent);
    }

    /**
     * Creates new instance of {@link HashParameters} as a concatenation of provided parameters.
     * Values for keys found across the provided parameters are "concatenated" into a {@link List} entry for their respective key
     * in the created {@link HashParameters} instance.
     *
     * @param parameters parameters to concatenate.
     * @return a new instance of {@link HashParameters} that represents the concatenation of the provided parameters.
     */
    public static HashParameters concat(Parameters... parameters) {
        return concat(new ArrayIterable<>(parameters));
    }

    /**
     * Creates new instance of {@link HashParameters} as a concatenation of provided parameters.
     * Values for keys found across the provided parameters are "concatenated" into a {@link List} entry for their respective key
     * in the created {@link HashParameters} instance.
     *
     * @param parameters parameters to concatenate.
     * @return a new instance of {@link HashParameters} that represents the concatenation of the provided parameters.
     */
    public static HashParameters concat(Iterable<Parameters> parameters) {
        return concat(parameters, HashParameters::new, HashParameters::new);
    }

    protected static <T extends HashParameters> T concat(
            Iterable<? extends Iterable<Map.Entry<String, List<String>>>> contentSources,
            Supplier<T> emptyFactory,
            Function<Iterable<Map.Entry<String, List<String>>>, T> singletonFactory) {

        Iterator<? extends Iterable<Map.Entry<String, List<String>>>> sources = contentSources.iterator();
        if (!sources.hasNext()) {
            return emptyFactory.get();
        }
        Iterable<Map.Entry<String, List<String>>> source = sources.next();
        if (!sources.hasNext()) {
            return singletonFactory.apply(source);
        }

        Map<String, List<String>> composer = new HashMap<>(); // source was initialized above
        do {
            if (source != null) {
                source.forEach(entry -> {
                    List<String> strings = composer.computeIfAbsent(entry.getKey(),
                                                                    k -> new ArrayList<>(entry.getValue().size()));
                    strings.addAll(entry.getValue());
                });
            }
            if (!sources.hasNext()) {
                break;
            }
            source = sources.next();
        } while(true);

        return singletonFactory.apply(composer.entrySet());
    }

    protected Map<String, List<String>> emptyMapForUpdates() {
        return new ConcurrentSkipListMap<>();
    }

    protected Map<String, List<String>> emptyMapForReads() {
        return new HashMap<>();
    }
}
