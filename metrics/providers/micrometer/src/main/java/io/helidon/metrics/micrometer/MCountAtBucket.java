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
package io.helidon.metrics.micrometer;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.distribution.CountAtBucket;

class MCountAtBucket implements io.helidon.metrics.api.CountAtBucket {

    static MCountAtBucket create(CountAtBucket delegate) {
        return new MCountAtBucket(delegate);
    }

    private final CountAtBucket delegate;

    private MCountAtBucket(CountAtBucket delegate) {
        this.delegate = delegate;
    }

    @Override
    public double bucket() {
        return delegate.bucket();
    }

    @Override
    public double bucket(TimeUnit unit) {
        return delegate.bucket(unit);
    }

    @Override
    public double count() {
        return delegate.count();
    }
}