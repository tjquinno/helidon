/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class NamedValueListsImpl<T extends NamedValueLists<T>> implements NamedValueLists<T> {

    private static final List<String> EMPTY_STRING_LIST = Collections.emptyList();

    private final Map<String, List<String>> content;
    private final T me;

    protected NamedValueListsImpl() {
        content = emptyMapForUpdates();
        me = (T) this;
    }

    protected NamedValueListsImpl(Map<String, List<String>> initialContent) {
        this(initialContent.entrySet());
    }

    protected NamedValueListsImpl(Iterable<Map.Entry<String, List<String>>> initialContent) {
        this();
        if (initialContent != null) {
            initialContent.forEach(entry ->
                                           content.compute(
                                                   entry.getKey(),
                                                   (key, values) -> {
                                                       if (values == null) {
                                                           return Collections.unmodifiableList(new ArrayList<>(entry.getValue()));
                                                       } else {
                                                           values.addAll(entry.getValue());
                                                           return values;

                                                       }
                                                   }
                                           ));
        }
    }
    @Override
    public Optional<String> first(String name) {
        Objects.requireNonNull(name, "Parameter 'name' is null!");
        return content.getOrDefault(name, EMPTY_STRING_LIST).stream().findFirst();
    }

    @Override
    public List<String> all(String name) {
        Objects.requireNonNull(name, "Parameter 'name' is null!");
        return content.getOrDefault(name, EMPTY_STRING_LIST);
    }

    @Override
    public List<String> put(String key, String... values) {
        List<String> vs = internalListCopy(values);
        List<String> result;
        if (vs == null) {
            result = content.remove(key);
        } else {
            result = content.put(key, vs);
        }
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> put(String key, Iterable<String> values) {
        List<String> vs = internalListCopy(values);
        List<String> result;
        if (vs == null) {
            result = content.remove(key);
        } else {
            result = content.put(key, vs);
        }
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> putIfAbsent(String key, String... values) {
        List<String> vls = internalListCopy(values);
        List<String> result;
        if (vls != null) {
            result = content.putIfAbsent(key, vls);
        } else {
            result = content.get(key);
        }
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> putIfAbsent(String key, Iterable<String> values) {
        List<String> vls = internalListCopy(values);
        List<String> result;
        if (vls != null) {
            result = content.putIfAbsent(key, vls);
        } else {
            result = content.get(key);
        }
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> computeIfAbsent(String key, Function<String, Iterable<String>> values) {
        List<String> result = content.computeIfAbsent(key, k -> internalListCopy(values.apply(k)));
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<String> computeSingleIfAbsent(String key, Function<String, String> value) {
        List<String> result = content.computeIfAbsent(key, k -> {
            String v = value.apply(k);
            if (v == null) {
                return null;
            } else {
                return Collections.singletonList(v);
            }
        });
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public T putAll(T parameters) {
        if (parameters == null) {
            return me;
        }

        for (Map.Entry<String, List<String>> entry : parameters.toMap().entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                content.put(entry.getKey(), Collections.unmodifiableList(values));
            }
        }
        return me;
    }

    @Override
    public T add(String key, String... values) {
        Objects.requireNonNull(key, "Parameter 'key' is null!");
        if (values == null || values.length == 0) {
            // do not necessarily create an entry in the map, simply immediately return
            return me;
        }

        content.compute(key, (s, list) -> {
            if (list == null) {
                return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
            } else {
                ArrayList<String> newValues = new ArrayList<>(list.size() + values.length);
                newValues.addAll(list);
                newValues.addAll(Arrays.asList(values));
                return Collections.unmodifiableList(newValues);
            }
        });
        return me;
    }

    @Override
    public T add(String key, Iterable<String> values) {
        Objects.requireNonNull(key, "Parameter 'key' is null!");
        List<String> vls = internalListCopy(values);
        if (vls == null) {
            // do not necessarily create an entry in the map, simply immediately return
            return me;
        }

        content.compute(key, (s, list) -> {
            if (list == null) {
                return Collections.unmodifiableList(vls);
            } else {
                ArrayList<String> newValues = new ArrayList<>(list.size() + vls.size());
                newValues.addAll(list);
                newValues.addAll(vls);
                return Collections.unmodifiableList(newValues);
            }
        });
        return me;
    }

    @Override
    public T addAll(T parameters) {
        if (parameters == null) {
            return me;
        }
        Map<String, List<String>> map = parameters.toMap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return me;
    }

    @Override
    public List<String> remove(String key) {
        List<String> result = content.remove(key);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public Map<String, List<String>> toMap() {
        // deep copy
        Map<String, List<String>> result = emptyMapForReads();
        for (Map.Entry<String, List<String>> entry : content.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    @Override
    public String toString() {
        return content.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamedValueListsImpl<?> entries = (NamedValueListsImpl<?>) o;
        return content.equals(entries.content);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + 37 * content.hashCode();
    }

    @Override
    public Iterator<Map.Entry<String, List<String>>> iterator() {
        return content.entrySet().iterator();
    }

    protected abstract Map<String, List<String>> emptyMapForUpdates();
    protected abstract Map<String, List<String>> emptyMapForReads();

    /**
     * {@code Iterable} around an array (to avoid {@code Array.asList}).
     * @param <T> type of the array elements
     */
    protected static class ArrayIterable<T> implements Iterable<T> {

        private final T[] content;

        protected ArrayIterable(T[] content) {
            this.content = content;
        }

        @Override
        public Iterator<T> iterator() {
            return content == null ? Collections.emptyIterator() : new Iterator<>() {

                private int slot = 0;
                @Override
                public boolean hasNext() {
                    return slot < content.length;
                }

                @Override
                public T next() {
                    if (slot >= content.length) {
                        throw new NoSuchElementException();
                    }
                    return content[slot++];
                }
            };
        }



        @Override
        public void forEach(Consumer<? super T> action) {
            for (T t : content) {
                action.accept(t);
            }
        }
    }

    private List<String> internalListCopy(String... values) {
        return Optional.ofNullable(values)
                .map(Arrays::asList)
                .filter(l -> !l.isEmpty())
                .map(Collections::unmodifiableList)
                .orElse(null);
    }

    private List<String> internalListCopy(Iterable<String> values) {
        if (values == null) {
            return null;
        } else {
            List<String> result;
            if (values instanceof Collection) {
                result = new ArrayList<>((Collection<String>) values);
            } else {
                result = new ArrayList<>();
                for (String value : values) {
                    result.add(value);
                }
            }
            if (result.isEmpty()) {
                return null;
            } else {
                return Collections.unmodifiableList(result);
            }
        }
    }


}
