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
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures receiver-profile based devirtualization for interface calls.
 *
 * The call profile is controlled by the receiver array:
 *   monomorphicA/B/C/D: one receiver class, split to avoid depending on
 *                       whichever receiver is cheapest in the lookup path
 *   bimorphic:   two receiver classes
 *   major:       one dominant receiver plus several cold receivers
 *   megamorphic: four receiver classes, used as the no-devirtualization baseline
 *
 * The dispatch benchmarks use a tiny callee to make virtual dispatch cost visible.
 *
 * Enable receiver devirtualization:
 *   VM_OPTIONS="-XX:+JeandleUseProfile -XX:+UseTypeProfile -XX:-Inline"
 *
 * Disable receiver devirtualization for the baseline:
 *   VM_OPTIONS="-XX:+JeandleUseProfile -XX:-UseTypeProfile -XX:-Inline"
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class JeandleReceiverPGO {
    private static final int INVOCATIONS = 1 << 16;

    public interface Receiver {
        int dispatch(int x);
    }

    public static class ReceiverA implements Receiver {
        public int dispatch(int x) {
            return x + 1;
        }
    }

    public static class ReceiverB implements Receiver {
        public int dispatch(int x) {
            return x + 3;
        }
    }

    public static class ReceiverC implements Receiver {
        public int dispatch(int x) {
            return x + 5;
        }
    }

    public static class ReceiverD implements Receiver {
        public int dispatch(int x) {
            return x + 7;
        }
    }

    private final Receiver[] monomorphicA = new Receiver[INVOCATIONS];
    private final Receiver[] monomorphicB = new Receiver[INVOCATIONS];
    private final Receiver[] monomorphicC = new Receiver[INVOCATIONS];
    private final Receiver[] monomorphicD = new Receiver[INVOCATIONS];
    private final Receiver[] bimorphic = new Receiver[INVOCATIONS];
    private final Receiver[] majorReceiver = new Receiver[INVOCATIONS];
    private final Receiver[] megamorphic = new Receiver[INVOCATIONS];
    private final int[] values = new int[INVOCATIONS];

    @Setup
    public void setup() {
        Receiver a = new ReceiverA();
        Receiver b = new ReceiverB();
        Receiver c = new ReceiverC();
        Receiver d = new ReceiverD();
        for (int i = 0; i < INVOCATIONS; i++) {
            values[i] = i * 17 + 31;
            monomorphicA[i] = a;
            monomorphicB[i] = b;
            monomorphicC[i] = c;
            monomorphicD[i] = d;
            bimorphic[i] = (i & 1) == 0 ? c : d;

            // 60/64 calls use C, while the remaining calls are spread across
            // A/B/D. This should avoid mono/bimorphic classification but still
            // satisfy TypeProfileMajorReceiverPercent's default 90% threshold.
            int majorCase = i & 63;
            if (majorCase < 60) {
                majorReceiver[i] = c;
            } else if (majorCase == 60) {
                majorReceiver[i] = a;
            } else if (majorCase == 61) {
                majorReceiver[i] = b;
            } else {
                majorReceiver[i] = d;
            }

            switch (i & 3) {
                case 0 -> megamorphic[i] = a;
                case 1 -> megamorphic[i] = b;
                case 2 -> megamorphic[i] = c;
                default -> megamorphic[i] = d;
            }
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callDispatchMonomorphic(Receiver receiver, int x) {
        return receiver.dispatch(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callDispatchBimorphic(Receiver receiver, int x) {
        return receiver.dispatch(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callDispatchMajorReceiver(Receiver receiver, int x) {
        return receiver.dispatch(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callDispatchMegamorphic(Receiver receiver, int x) {
        return receiver.dispatch(x);
    }

    @Benchmark
    public int dispatchMonomorphicA() {
        int sum = 0;
        Receiver[] receivers = monomorphicA;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMonomorphic(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchMonomorphicB() {
        int sum = 0;
        Receiver[] receivers = monomorphicB;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMonomorphic(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchMonomorphicC() {
        int sum = 0;
        Receiver[] receivers = monomorphicC;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMonomorphic(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchMonomorphicD() {
        int sum = 0;
        Receiver[] receivers = monomorphicD;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMonomorphic(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchBimorphic() {
        int sum = 0;
        Receiver[] receivers = bimorphic;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchBimorphic(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchMajorReceiver() {
        int sum = 0;
        Receiver[] receivers = majorReceiver;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMajorReceiver(receivers[i], input[i]);
        }
        return sum;
    }

    @Benchmark
    public int dispatchMegamorphic() {
        int sum = 0;
        Receiver[] receivers = megamorphic;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMegamorphic(receivers[i], input[i]);
        }
        return sum;
    }
}
