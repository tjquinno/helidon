/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.metrics.providers.micrometer;

import java.util.Objects;
import java.util.StringJoiner;

import io.micrometer.core.instrument.distribution.CountAtBucket;

class MCountAtBucket implements io.helidon.metrics.api.CountAtBucket {

    private final CountAtBucket delegate;

    MCountAtBucket(CountAtBucket delegate) {
        this.delegate = delegate;
    }

    static MCountAtBucket create(CountAtBucket delegate) {
        return new MCountAtBucket(delegate);
    }

    @Override
    public double bucket() {
        return delegate.bucket();
    }

    @Override
    public double count() {
        return delegate.count();
    }

    @Override
    public <R> R unwrap(Class<? extends R> c) {
        return c.cast(delegate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof io.helidon.metrics.api.CountAtBucket that)) {
            return false;
        }
        return Objects.equals(delegate.bucket(), that.bucket())
                && Objects.equals(delegate.count(), that.count());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.hash(delegate.bucket(), delegate.count()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("bucket=" + bucket())
                .add("count=" + count())
                .toString();
    }
}
