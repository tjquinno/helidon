/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Parameters represents {@code key : value} pairs where {@code key} is a {@code String} with potentially multiple values.
 * <p>
 * This structure represents query parameters, headers and path parameters in e.g. {@link HttpRequest}.
 * <p>
 * Interface focus on most convenient use cases in HTTP Request and Response processing, like
 * <pre>
 * {@code
 * // Get and map with default
 * .first("count").map(Integer::new).orElse(0);
 * // Find max in multiple values
 * .all("counts").stream().mapToInt(Integer::valueOf).max().orElse(0);
 * }
 * </pre>
 * <p>
 * Mutable operations are defined in two forms:
 * <ul>
 * <li>{@code put...} create or replace association.</li>
 * <li>{@code add...} create association or add values to existing association.</li>
 * </ul>
 * <p>
 * It is possible to use {@link #toMap()} method to get immutable map view of data.
 * <p>
 * Various static factory methods can be used to create common implementations.
 */
public interface Parameters extends NamedValueLists<Parameters>, Iterable<Map.Entry<String, List<String>>> {

    /**
     * Returns an unmodifiable view.
     *
     * @param parameters a parameters for unmodifiable view.
     * @return An unmodifiable view.
     * @throws NullPointerException if parameter {@code parameters} is null.
     */
    static Parameters toUnmodifiableParameters(Parameters parameters) {
        Objects.requireNonNull(parameters, "Parameter 'parameters' is null!");
        return new UnmodifiableParameters(parameters);
    }
}
