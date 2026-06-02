/*
 * Copyright (c) 2026, the Jeandle-JDK Authors. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package org.openjdk.bench.jeandle;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class JeandleBranchPGO {
    private static final int INVOCATIONS = 1_048_576;

    private final int[] mostlySmall = new int[INVOCATIONS];
    private final int[] alternating = new int[INVOCATIONS];

    @Setup
    public void setup() {
        for (int i = 0; i < mostlySmall.length; i++) {
            mostlySmall[i] = (i % 100 == 0) ? 4096 : (i & 1023);
            alternating[i] = (i & 1) == 0 ? 4096 : (i & 1023);
        }
    }

    @Benchmark
    public int biasedBranch() {
        int sum = 0;
        for (int value : mostlySmall) {
            if (value < 1024) {
                sum += value + 1;
            } else {
                sum += value - 1;
            }
        }
        return sum;
    }

    @Benchmark
    public int balancedBranch() {
        int sum = 0;
        for (int value : alternating) {
            if (value < 1024) {
                sum += value + 1;
            } else {
                sum += value - 1;
            }
        }
        return sum;
    }
}
