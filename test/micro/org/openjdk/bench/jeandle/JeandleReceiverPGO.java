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
 *   monomorphic: one receiver class
 *   bimorphic:   two receiver classes
 *   megamorphic: four receiver classes, used as the no-devirtualization baseline
 *
 * The dispatch benchmarks use a tiny callee to make virtual dispatch cost visible.
 * The inlineAmplified benchmarks use a larger callee body; once receiver PGO turns
 * the call into a guarded direct call, method inlining can remove the call and
 * expose the callee body to later optimizations.
 *
 * Example:
 *   make test TEST="micro:org.openjdk.bench.jeandle.JeandleReceiverPGO"
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
        int inlineAmplified(int x);
    }

    public static class ReceiverA implements Receiver {
        public int dispatch(int x) {
            return x + 1;
        }

        public int inlineAmplified(int x) {
            return mix(x, 0x13579bdf);
        }
    }

    public static class ReceiverB implements Receiver {
        public int dispatch(int x) {
            return x + 3;
        }

        public int inlineAmplified(int x) {
            return mix(x, 0x2468ace1);
        }
    }

    public static class ReceiverC implements Receiver {
        public int dispatch(int x) {
            return x + 5;
        }

        public int inlineAmplified(int x) {
            return mix(x, 0x10203040);
        }
    }

    public static class ReceiverD implements Receiver {
        public int dispatch(int x) {
            return x + 7;
        }

        public int inlineAmplified(int x) {
            return mix(x, 0x55667788);
        }
    }

    private final Receiver[] monomorphic = new Receiver[INVOCATIONS];
    private final Receiver[] bimorphic = new Receiver[INVOCATIONS];
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
            monomorphic[i] = a;
            bimorphic[i] = (i & 1) == 0 ? a : b;
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
    private static int callDispatchMegamorphic(Receiver receiver, int x) {
        return receiver.dispatch(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callInlineAmplifiedMonomorphic(Receiver receiver, int x) {
        return receiver.inlineAmplified(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callInlineAmplifiedBimorphic(Receiver receiver, int x) {
        return receiver.inlineAmplified(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int callInlineAmplifiedMegamorphic(Receiver receiver, int x) {
        return receiver.inlineAmplified(x);
    }

    private static int mix(int x, int salt) {
        int y = x ^ salt;
        y += y << 3;
        y ^= y >>> 11;
        y += y << 15;
        return y;
    }

    @Benchmark
    public int dispatchMonomorphic() {
        int sum = 0;
        Receiver[] receivers = monomorphic;
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
    public int dispatchMegamorphic() {
        int sum = 0;
        Receiver[] receivers = megamorphic;
        int[] input = values;
        for (int i = 0; i < receivers.length; i++) {
            sum += callDispatchMegamorphic(receivers[i], input[i]);
        }
        return sum;
    }

    // @Benchmark
    // public int inlineAmplifiedMonomorphic() {
    //     int sum = 0;
    //     Receiver[] receivers = monomorphic;
    //     int[] input = values;
    //     for (int i = 0; i < receivers.length; i++) {
    //         sum += callInlineAmplifiedMonomorphic(receivers[i], input[i]);
    //     }
    //     return sum;
    // }

    // @Benchmark
    // public int inlineAmplifiedBimorphic() {
    //     int sum = 0;
    //     Receiver[] receivers = bimorphic;
    //     int[] input = values;
    //     for (int i = 0; i < receivers.length; i++) {
    //         sum += callInlineAmplifiedBimorphic(receivers[i], input[i]);
    //     }
    //     return sum;
    // }

    // @Benchmark
    // public int inlineAmplifiedMegamorphic() {
    //     int sum = 0;
    //     Receiver[] receivers = megamorphic;
    //     int[] input = values;
    //     for (int i = 0; i < receivers.length; i++) {
    //         sum += callInlineAmplifiedMegamorphic(receivers[i], input[i]);
    //     }
    //     return sum;
    // }
}
