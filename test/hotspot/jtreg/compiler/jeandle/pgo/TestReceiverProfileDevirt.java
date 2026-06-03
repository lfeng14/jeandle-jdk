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
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

/*
 * @test
 * @summary Test monomorphic receiver profile devirtualization in Jeandle
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox compiler.jeandle.fileCheck.FileCheck
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestReceiverProfileDevirt::virtualCallTarget
 *      compiler.jeandle.pgo.TestReceiverProfileDevirt
 */

package compiler.jeandle.pgo;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import compiler.jeandle.fileCheck.FileCheck;

import jdk.test.lib.Asserts;
import jdk.test.whitebox.WhiteBox;

public class TestReceiverProfileDevirt {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int WARMUP = 20_000;
    
    // Base class for hierarchy
    static class Base {
        public int foo() { return 42; }
    }
    
    // Derived class 1
    static class Derived1 extends Base {
        public int foo() { return 100; }
    }
    
    // Derived class 2
    static class Derived2 extends Base {
        public int foo() { return 200; }
    }
    
    // Virtual call site that will be devirtualized
    public static int virtualCallTarget(Base obj) {
        return obj.foo();
    }
    
    public static void main(String[] args) throws Exception {
        // Warm up with only Derived1 to create monomorphic profile
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(virtualCallTarget(new Derived1()), 100);
        }
        
        Method method = compile("virtualCallTarget", Base.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "Method should be compiled");
        
        // Check that we get the devirtualized result for Derived1
        int result = virtualCallTarget(new Derived1());
        Asserts.assertEquals(result, 100, "Should get correct result for Derived1");
        
        // Now test with Derived2 to trigger deoptimization
        int trapsBefore = WB.getMethodTrapCount(method, "speculate_class_check");
        int result2 = virtualCallTarget(new Derived2());
        Asserts.assertEquals(result2, 200, "Should get correct result for Derived2");
        int trapsAfter = WB.getMethodTrapCount(method, "speculate_class_check");
        
        Asserts.assertGTE(trapsAfter, trapsBefore + 1,
                "Should have triggered speculate_class_check trap when receiver type doesn't match profile");
        
        // Check IR to verify devirtualization happened
        checkDevirtualizedIR();
    }
    
    private static void checkDevirtualizedIR() throws Exception {
        FileCheck check = new FileCheck(System.getProperty("user.dir"),
                TestReceiverProfileDevirt.class.getDeclaredMethod("virtualCallTarget", Base.class),
                false);
        
        // Check that we have a type check on the receiver
        check.checkPattern("icmp .* ptr addrspace\(0\) %.*, ptr addrspace\(0\) @_ZTVN12_GLOBAL__N_18Derived1E");
        
        // Check that we have a branch based on the type check
        check.checkPattern("br i1 .*, label %.*, label %.*");
        
        // Check that we have the fast path call to Derived1.foo()
        check.checkPattern("call .*@_ZN12_GLOBAL__N_18Derived13fooEv");
        
        // Check that we have the slow path call to virtual call
        check.checkPattern("call .*@jeandle.resolve_opt_virtual_call");
    }
    
    private static Method compile(String name, Class<?>... parameterTypes) throws Exception {
        return compile(TestReceiverProfileDevirt.class.getDeclaredMethod(name, parameterTypes));
    }
    
    private static Method compile(Method method) throws Exception {
        if (!WB.enqueueMethodForCompilation(method, 4)) {
            throw new RuntimeException("enqueue failed for " + method);
        }
        while (!WB.isMethodCompiled(method)) {
            Thread.yield();
        }
        return method;
    }
}
