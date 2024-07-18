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
package io.helidon.metrics.api;

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

abstract class DistributionSettingImpl<T extends Comparable<T>> implements DistributionSetting<T> {

    private final String prefixOrExactName;
    private final boolean hasWildcardSuffix;
    private final T[] values;

    protected DistributionSettingImpl(String nameExpressionAndValues, Class<T> type) {
        int eq = nameExpressionAndValues.indexOf("=");
        if (eq <= 0) {
            throw new IllegalArgumentException("DistributionSetting must conform to 'nameExpression=optionalValuesCommaList'");
        }
        hasWildcardSuffix = nameExpressionAndValues.charAt(eq - 1) == '*';
        prefixOrExactName = nameExpressionAndValues.substring(0, hasWildcardSuffix ? eq - 1 : eq);

        String[] textValues = nameExpressionAndValues.substring(eq + 1).split(",");

        T[] result = (T[]) Array.newInstance(type, textValues.length);
        boolean inOrder = true;
        int nextSlot = 0;
        for (String textValue : textValues) {
            T candidateValue = valueParser().apply(textValue);
            if (candidateValue != null) {
                result[nextSlot] = candidateValue;
                if (nextSlot > 0 && inOrder) {
                    inOrder = result[nextSlot - 1].compareTo(candidateValue) < 0;
                }
                nextSlot++;
            }
        }
        if (!inOrder) {
            throw new IllegalArgumentException("value settings must be in ascending order but are not: "
                                                       + Arrays.toString(textValues));
        }
        values = Arrays.copyOf(result, nextSlot);
    }

    @Override
    public boolean matches(String name) {
        return hasWildcardSuffix
                ? name.startsWith(prefixOrExactName)
                : name.equals(prefixOrExactName);
    }

    protected abstract Function<String, T> valueParser();

    protected T[] boxedValues() {
        return values;
    }

    protected static double[] unboxedDoubles(Double[] original) {
        double[] result = new double[original.length];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i];
        }
        return result;
    }

    static class PercentilesImpl extends DistributionSettingImpl<Double> implements DistributionSetting.Percentiles {

        private final double[] values;

        PercentilesImpl(String nameExpressionAndValues) {
            super(nameExpressionAndValues, Double.class);
            values = unboxedDoubles(boxedValues());
        }

        @Override
        public double[] values() {
            return values;
        }

        @Override
        protected Function<String, Double> valueParser() {
            return value -> {
                if (value.isBlank()) {
                    return null;
                }
                double d = Double.parseDouble(value);
                if (d < 0.0 || d > 1.0) {
                    throw new IllegalArgumentException("percentile value " + d + " is out of range [0.0, 1.0]");
                }
                return d;
            };
        }
    }

    static class SummaryBucketsImpl extends DistributionSettingImpl<Double> implements DistributionSetting.SummaryBuckets {

        private final double[] values;

        SummaryBucketsImpl(String nameExpressionAndValues) {
            super(nameExpressionAndValues, Double.class);
            values = unboxedDoubles(boxedValues());
        }

        @Override
        protected Function<String, Double> valueParser() {
            return value -> {
                if (value.isBlank()) {
                    return null;
                }
                double d = Float.parseFloat(value);
                if (d <= 0) {
                    throw new IllegalArgumentException("distribution summary bucket value " + d + " must be greater than 0");
                }
                return d;
            };
        }

        @Override
        public double[] values() {
            return values;
        }
    }

    static class TimerBucketsImpl extends DistributionSettingImpl<Duration> implements DistributionSetting.TimerBuckets {

        TimerBucketsImpl(String nameExpressionAndValues) {
            super(nameExpressionAndValues, Duration.class);
        }

        @Override
        protected Function<String, Duration> valueParser() {
            /*
             The format is inspired by MicroProfile Metrics 5.1 which specifies that the numeric values must be integers (not
             decimals) and that only ms, s, m, and h are permitted as trailing unit indicators, not a mixture. Otherwise we
             could have used Duration.parse to process each value.
             */
            return timeExpr -> {
                if (timeExpr.isBlank()) {
                    return null;
                }
                var lowerCase = timeExpr.toLowerCase(Locale.ROOT);
                int suffixLength = 1;
                TimeUnit timeUnit;
                if (lowerCase.endsWith("ms")) {
                    timeUnit = TimeUnit.MILLISECONDS;
                    suffixLength = 2;
                } else if (lowerCase.endsWith("s")) {
                    timeUnit = TimeUnit.SECONDS;
                } else if (lowerCase.endsWith("m")) {
                    timeUnit = TimeUnit.MINUTES;
                } else if (lowerCase.endsWith("h")) {
                    timeUnit = TimeUnit.HOURS;
                } else {
                    timeUnit = TimeUnit.MILLISECONDS;
                    suffixLength = 0;
                }
                long amount = Long.parseLong(lowerCase.substring(0, lowerCase.length() - suffixLength));
                return Duration.of(amount, timeUnit.toChronoUnit());
            };
        }

        @Override
        public Duration[] values() {
            return boxedValues();
        }
    }
}
