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
 * @summary Exercise Jeandle branch profile weights and cold branch uncommon traps.
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox compiler.jeandle.fileCheck.FileCheck
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::branchTarget
 *      compiler.jeandle.pgo.TestPGOBase branch
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+WhiteBoxAPI -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:PerMethodTrapLimit=1
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::wholeMethodTrapTarget
 *      compiler.jeandle.pgo.TestPGOBase wholeMethodTrap
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:PerMethodRecompilationCutoff=1
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::recompileLimitTarget
 *      compiler.jeandle.pgo.TestPGOBase tooManyRecompiles
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:PerBytecodeRecompilationCutoff=1 -XX:PerMethodRecompilationCutoff=10000
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::perBciRecompileTarget
 *      compiler.jeandle.pgo.TestPGOBase perBciRecompiles
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::staticBranchTarget
 *      compiler.jeandle.pgo.TestPGOBase staticFallback
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:-JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::profileFlagOffTarget
 *      compiler.jeandle.pgo.TestPGOBase profileFlagOff
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::deoptResultBranchesTarget
 *      compiler.jeandle.pgo.TestPGOBase resultBranches
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xcomp -XX:-UseInterpreter -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::useInterpreterGateTarget
 *      compiler.jeandle.pgo.TestPGOBase useInterpreterGate
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::virtualCallTarget
 *      compiler.jeandle.pgo.TestPGOBase receiverDevirt
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::bimorphicCallTarget
 *      compiler.jeandle.pgo.TestPGOBase bimorphicDevirt
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::majorReceiverCallTarget
 *      compiler.jeandle.pgo.TestPGOBase majorReceiverDevirt
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::interfaceCallTarget
 *      compiler.jeandle.pgo.TestPGOBase interfaceReceiverDevirt
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+WhiteBoxAPI -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler
 *      -XX:+JeandleUseProfile -XX:+JeandleDumpIR -XX:PerMethodTrapLimit=0
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::receiverFallbackTarget
 *      compiler.jeandle.pgo.TestPGOBase receiverFallback
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -Xbatch -XX:-BackgroundCompilation -XX:+UseJeandleCompiler -XX:+JeandleUseProfile
 *      -XX:-UseTypeProfile -XX:+JeandleDumpIR
 *      -XX:CompileCommand=compileonly,compiler.jeandle.pgo.TestPGOBase::receiverTypeProfileOffTarget
 *      compiler.jeandle.pgo.TestPGOBase receiverTypeProfileOff
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

public class TestPGOBase {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int WARMUP = 20_000;

    public static void main(String[] args) throws Exception {
        String testName = args.length == 0 ? "branch" : args[0];
        switch (testName) {
            case "branch" -> testBranchColdTrap();
            case "wholeMethodTrap" -> testWholeMethodTrapLimit();
            case "tooManyRecompiles" -> testTooManyRecompilesLimit();
            case "perBciRecompiles" -> testPerBciRecompilesLimit();
            case "staticFallback" -> testStaticPredictionFallback();
            case "profileFlagOff" -> testProfileFlagOff();
            case "resultBranches" -> testDeoptResultBranches();
            case "useInterpreterGate" -> testUseInterpreterGate();
            case "receiverDevirt" -> testReceiverDevirt();
            case "bimorphicDevirt" -> testBimorphicDevirt();
            case "majorReceiverDevirt" -> testMajorReceiverDevirt();
            case "interfaceReceiverDevirt" -> testInterfaceReceiverDevirt();
            case "receiverFallback" -> testReceiverFallback();
            case "receiverTypeProfileOff" -> testReceiverTypeProfileOff();
            default -> throw new IllegalArgumentException("unknown test: " + testName);
        }
    }

    private static void testBranchColdTrap() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(branchTarget(i), i + 1);
        }

        Method method = compile("branchTarget", int.class);
        Asserts.assertEquals(branchTarget(42), 43);

        int trapsBefore = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertEquals(branchTarget(-3), -5);
        int trapsAfter = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertGTE(trapsAfter, trapsBefore + 1,
                "cold branch should deopt through unstable_if uncommon trap");

        checkBranchIR();
    }

    private static void testWholeMethodTrapLimit() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(wholeMethodTrapTarget(i, i), wholeMethodTrapExpected(i, i));
        }

        Method method = compile("wholeMethodTrapTarget", int.class, int.class);

        int trapsBefore = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertEquals(wholeMethodTrapTarget(-3, 5), wholeMethodTrapExpected(-3, 5));
        int trapsAfterFirstColdPath = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertGTE(trapsAfterFirstColdPath, trapsBefore + 1,
                "first cold branch should deopt through unstable_if uncommon trap");

        WB.deoptimizeMethod(method);
        compile(method);

        Asserts.assertEquals(wholeMethodTrapTarget(5, -3), wholeMethodTrapExpected(5, -3));
        int trapsAfterSecondColdPath = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertEquals(trapsAfterSecondColdPath, trapsAfterFirstColdPath,
                "whole-method trap limit should disable further unstable_if speculation");

        checkProfileGuardDisabledIR("wholeMethodTrapTarget",
                "trap-limit fallback should stop emitting profile branch guard blocks");
    }

    private static void testTooManyRecompilesLimit() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(recompileLimitTarget(i, i), wholeMethodTrapExpected(i, i));
        }

        Method method = compile("recompileLimitTarget", int.class, int.class);

        int trapsBefore = WB.getMethodTrapCount(method, "unstable_if");
        int decompilesBefore = WB.getMethodDecompileCount(method);
        Asserts.assertEquals(recompileLimitTarget(-3, 5), wholeMethodTrapExpected(-3, 5));
        int trapsAfterFirstColdPath = WB.getMethodTrapCount(method, "unstable_if");
        int decompilesAfterFirstColdPath = WB.getMethodDecompileCount(method);
        Asserts.assertGTE(trapsAfterFirstColdPath, trapsBefore + 1,
                "first cold branch should deopt through unstable_if uncommon trap");
        Asserts.assertGTE(decompilesAfterFirstColdPath, decompilesBefore + 1,
                "first cold branch should record a recompilation/decompile event");

        compile(method);

        Asserts.assertEquals(recompileLimitTarget(5, -3), wholeMethodTrapExpected(5, -3));
        int trapsAfterSecondColdPath = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertEquals(trapsAfterSecondColdPath, trapsAfterFirstColdPath,
                "too_many_recompiles should disable further unstable_if speculation");

        checkProfileGuardDisabledIR("recompileLimitTarget",
                "too_many_recompiles fallback should stop emitting profile branch guard blocks");
    }

    private static void testPerBciRecompilesLimit() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(perBciRecompileTarget(i), branchExpected(i));
        }

        Method method = compile("perBciRecompileTarget", int.class);

        int trapsBefore = WB.getMethodTrapCount(method, "unstable_if");
        int decompilesBefore = WB.getMethodDecompileCount(method);
        Asserts.assertEquals(perBciRecompileTarget(-3), branchExpected(-3));
        int trapsAfterColdPath = WB.getMethodTrapCount(method, "unstable_if");
        int decompilesAfterColdPath = WB.getMethodDecompileCount(method);
        Asserts.assertGTE(trapsAfterColdPath, trapsBefore + 1,
                "first cold branch should deopt through unstable_if uncommon trap");
        Asserts.assertGTE(decompilesAfterColdPath, decompilesBefore + 1,
                "first cold branch should record a same-bci recompile event");

        compile(method);

        Asserts.assertEquals(perBciRecompileTarget(-4), branchExpected(-4));
        int trapsAfterSameBciColdPath = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertEquals(trapsAfterSameBciColdPath, trapsAfterColdPath,
                "per-bci recompile limit should disable same-bci unstable_if speculation");

        checkProfileGuardDisabledIR("perBciRecompileTarget",
                "per-bci recompile fallback should stop emitting profile branch guard blocks");
    }

    private static void testStaticPredictionFallback() throws Exception {
        Method method = compile("staticBranchTarget", int.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "static fallback target should compile");
        Asserts.assertEquals(staticBranchTarget(8), 7);

        checkStaticFallbackIR("staticBranchTarget");
    }


    private static void testProfileFlagOff() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(profileFlagOffTarget(i), i + 1);
        }

        Method method = compile("profileFlagOffTarget", int.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "profile flag-off target should compile");
        Asserts.assertEquals(profileFlagOffTarget(42), 43);

        checkProfileUseDisabledIR("profileFlagOffTarget");
    }

    private static void testDeoptResultBranches() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(deoptResultBranchesTarget(i, (i % 7) + 1),
                    deoptResultBranchesExpected(i, (i % 7) + 1));
        }

        Method method = compile("deoptResultBranchesTarget", int.class, int.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "result branch target should compile");

        checkDeoptResultBranchesIR();

        Asserts.assertEquals(deoptResultBranchesTarget(5, 4),
                deoptResultBranchesExpected(5, 4),
                "hot/no-throw branch result");
        Asserts.assertEquals(deoptResultBranchesTarget(5, 0),
                deoptResultBranchesExpected(5, 0),
                "hot/throw branch result");

        int trapsBefore = WB.getMethodTrapCount(method, "unstable_if");
        for (int i = 1; i <= 400; i++) {
            int x = -i;
            Asserts.assertEquals(deoptResultBranchesTarget(x, 3),
                    deoptResultBranchesExpected(x, 3),
                    "cold/no-throw branch result for x=" + x);
        }
        int trapsAfterColdPath = WB.getMethodTrapCount(method, "unstable_if");
        Asserts.assertGTE(trapsAfterColdPath, trapsBefore + 1,
                "cold branch should deopt through unstable_if uncommon trap");

        for (int i = 1; i <= 50; i++) {
            int x = -i;
            Asserts.assertEquals(deoptResultBranchesTarget(x, 0),
                    deoptResultBranchesExpected(x, 0),
                    "cold/throw branch result for x=" + x);
        }
    }

    private static void testUseInterpreterGate() throws Exception {
        Method method = compile("useInterpreterGateTarget", int.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "UseInterpreter gate target should compile");
        Asserts.assertEquals(useInterpreterGateTarget(8), 7);

        checkStaticFallbackIR("useInterpreterGateTarget");
    }

    private static void testReceiverDevirt() throws Exception {
        // Warm up with only DerivedA to create monomorphic profile
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(virtualCallTarget(new DerivedA()), 100);
        }

        Method method = compile("virtualCallTarget", Base.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "virtualCallTarget should compile");

        // Hot path: DerivedA receiver matches the profile
        Asserts.assertEquals(virtualCallTarget(new DerivedA()), 100,
                "Should get correct result for profiled receiver");

        // Miss path: DerivedB receiver triggers deopt
        int trapsBefore = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(virtualCallTarget(new DerivedB()), 200,
                "Should get correct result after deopt for unprofiled receiver");
        int trapsAfter = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertGTE(trapsAfter, trapsBefore + 1,
                "unprofiled receiver should trigger class_check deopt");

        checkReceiverDevirtIR();
    }

    private static void checkBranchIR() throws Exception {
        FileCheck branchCheck = new FileCheck(System.getProperty("user.dir"),
                TestPGOBase.class.getDeclaredMethod("branchTarget", int.class),
                false);
        branchCheck.checkPattern("br i1 .*label %bci_[0-9]+_profile_branch_hit, label %bci_[0-9]+_profile_branch_miss, !prof ![0-9]+");
        branchCheck.check("profile_branch_hit");
        branchCheck.check("profile_branch_miss");
        branchCheck.checkPattern("call .*@llvm\\.experimental\\.deoptimize");
        branchCheck.checkNextPattern("ret i32");
        branchCheck.checkNotPattern("call .*@uncommon_trap");
        branchCheck.checkPattern("![0-9]+ = !\\{!\\\"branch_weights\\\", i32 [0-9]+, i32 [0-9]+\\}");
    }

    private static void checkDeoptResultBranchesIR() throws Exception {
        FileCheck pre = new FileCheck(System.getProperty("user.dir"),
                TestPGOBase.class.getDeclaredMethod("deoptResultBranchesTarget", int.class, int.class),
                false);
        pre.checkPattern("define hotspotcc i32 .*deoptResultBranchesTarget");
        pre.checkPattern("br i1 .*profile_branch");
        pre.checkPattern("call .*@llvm\\.experimental\\.deoptimize");
        pre.checkNextPattern("ret i32");
        pre.checkPattern("declare hotspotcc void @__llvm_deoptimize\\(i32\\)");
        pre.checkNotPattern("call .*@uncommon_trap");

        FileCheck opt = new FileCheck(System.getProperty("user.dir"),
                TestPGOBase.class.getDeclaredMethod("deoptResultBranchesTarget", int.class, int.class),
                true);
        opt.checkPattern("call hotspotcc token .*@llvm\\.experimental\\.gc\\.statepoint\\.p0.*@__llvm_deoptimize");
        opt.checkPattern("declare void @__llvm_deoptimize\\(i32\\)");
        opt.checkNotPattern("call .*@uncommon_trap");
    }

    private static void checkProfileGuardDisabledIR(String methodName, String message) throws Exception {
        List<String> lines = latestDumpLines(methodName);
        Asserts.assertTrue(containsPattern(lines, "![0-9]+ = !\\{!\\\"branch_weights\\\", i32 [0-9]+, i32 [0-9]+\\}"),
                "ordinary branch_weights should still be emitted after fallback");
        Asserts.assertFalse(contains(lines, "profile_branch_hit") || contains(lines, "profile_branch_miss"), message);
    }


    private static void checkProfileUseDisabledIR(String methodName) throws Exception {
        List<String> lines = latestDumpLines(methodName);
        Asserts.assertFalse(contains(lines, "profile_branch_hit") || contains(lines, "profile_branch_miss"),
                "-XX:-JeandleUseProfile should disable profile uncommon-trap guard blocks");
        Asserts.assertFalse(containsPattern(lines, "call .*@llvm\\.experimental\\.deoptimize"),
                "-XX:-JeandleUseProfile should not emit profile-driven deoptimize guards");
    }

    private static void checkStaticFallbackIR(String methodName) throws Exception {
        List<String> lines = latestDumpLines(methodName);
        Asserts.assertTrue(containsPattern(lines, "![0-9]+ = !\\{!\\\"branch_weights\\\", i32 1, i32 9\\}")
                        || containsPattern(lines, "![0-9]+ = !\\{!\\\"branch_weights\\\", i32 9, i32 1\\}"),
                "branch without usable profile should use C2-style static branch weights");
        Asserts.assertFalse(contains(lines, "profile_branch_hit") || contains(lines, "profile_branch_miss"),
                "static fallback should not emit profile uncommon-trap guard blocks");
    }

    private static void checkReceiverDevirtIR() throws Exception {
        FileCheck check = new FileCheck(System.getProperty("user.dir"),
                TestPGOBase.class.getDeclaredMethod("virtualCallTarget", Base.class),
                false);
        // Devirtualization emits a klass guard branch
        check.checkPattern("br i1 .*label %bci_[0-9]+_profile_receiver_hit, label %bci_[0-9]+_profile_receiver_miss");
        check.check("profile_receiver_hit");
        check.check("profile_receiver_miss");
        // Miss path deopts via llvm.experimental.deoptimize, not uncommon_trap
        check.checkPattern("call .*@llvm\\.experimental\\.deoptimize");
        check.checkNotPattern("call .*@uncommon_trap");
    }

    private static void testBimorphicDevirt() throws Exception {
        // Warm up with both DerivedA and DerivedB to create bimorphic profile
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(bimorphicCallTarget(new DerivedA()), 100);
            Asserts.assertEquals(bimorphicCallTarget(new DerivedB()), 200);
        }

        Method method = compile("bimorphicCallTarget", Base.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "bimorphicCallTarget should compile");

        // Hot path: both receivers should be handled by fast paths (no deopt)
        int trapsBefore = bimorphicDeoptCount();
        Asserts.assertEquals(bimorphicCallTarget(new DerivedA()), 100,
                "Should get correct result for first profiled receiver");
        Asserts.assertEquals(bimorphicCallTarget(new DerivedB()), 200,
                "Should get correct result for second profiled receiver");
        int trapsAfter = bimorphicDeoptCount();
        Asserts.assertEquals(trapsAfter, trapsBefore,
                "profiled receivers should not trigger deopt");

        // Miss path: unprofiled receiver triggers deopt
        Asserts.assertEquals(bimorphicCallTarget(new Base()), 42,
                "Should get correct result after deopt for unprofiled receiver");
        int trapsAfterMiss = bimorphicDeoptCount();
        Asserts.assertGTE(trapsAfterMiss, trapsBefore + 1,
                "unprofiled receiver should trigger bimorphic deopt");

        checkBimorphicDevirtIR();
    }

    private static void testMajorReceiverDevirt() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Base receiver = switch (i % 40) {
                case 38 -> new DerivedB();
                case 39 -> new DerivedC();
                default -> new DerivedA();
            };
            Asserts.assertEquals(majorReceiverCallTarget(receiver), receiver.foo());
        }

        Method method = compile("majorReceiverCallTarget", Base.class);
        Asserts.assertTrue(WB.isMethodCompiled(method), "majorReceiverCallTarget should compile");
        checkMajorReceiverIR();

        int trapsBefore = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(majorReceiverCallTarget(new DerivedB()), 200);
        Asserts.assertEquals(majorReceiverCallTarget(new DerivedC()), 300);
        int trapsAfter = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(trapsAfter, trapsBefore,
                "minor receivers must use the dynamic fallback without deoptimizing");
        Asserts.assertTrue(WB.isMethodCompiled(method),
                "major receiver fallback should keep the method compiled");
    }

    private static void testInterfaceReceiverDevirt() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(interfaceCallTarget(new InterfaceA()), 400);
        }

        Method method = compile("interfaceCallTarget", ReceiverInterface.class);
        int trapsBefore = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(interfaceCallTarget(new InterfaceB()), 500);
        int trapsAfter = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertGTE(trapsAfter, trapsBefore + 1,
                "unprofiled interface receiver should trigger class_check deopt");
        checkInterfaceReceiverIR();
    }

    private static void testReceiverFallback() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(receiverFallbackTarget(new DerivedA()), 100);
        }

        Method method = compile("receiverFallbackTarget", Base.class);
        checkReceiverFallbackIR();
        int trapsBefore = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(receiverFallbackTarget(new DerivedB()), 200);
        int trapsAfter = WB.getMethodTrapCount(method, "class_check");
        Asserts.assertEquals(trapsAfter, trapsBefore,
                "a throttled class check must use the dynamic fallback");
        Asserts.assertTrue(WB.isMethodCompiled(method),
                "receiver fallback should keep the method compiled");
    }

    private static void testReceiverTypeProfileOff() throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            Asserts.assertEquals(receiverTypeProfileOffTarget(new DerivedA()), 100);
        }

        compile("receiverTypeProfileOffTarget", Base.class);
        List<String> lines = latestDumpLines("receiverTypeProfileOffTarget");
        Asserts.assertFalse(contains(lines, "profile_receiver"),
                "-XX:-UseTypeProfile should disable receiver-profile devirtualization");
    }

    private static void checkBimorphicDevirtIR() throws Exception {
        FileCheck check = new FileCheck(System.getProperty("user.dir"),
                TestPGOBase.class.getDeclaredMethod("bimorphicCallTarget", Base.class),
                false);
        // Two cascaded klass guards
        check.check("profile_receiver_0_hit");
        check.check("profile_receiver_0_miss");
        check.check("profile_receiver_1_hit");
        check.check("profile_receiver_1_miss");
        // Miss path deopts via llvm.experimental.deoptimize (appears before merge in IR)
        check.checkPattern("call .*@llvm\\.experimental\\.deoptimize");
        // Merge block
        check.check("bimorphic_merge");
        check.checkNotPattern("call .*@uncommon_trap");
    }

    private static void checkMajorReceiverIR() throws Exception {
        List<String> lines = latestDumpLines("majorReceiverCallTarget");
        Asserts.assertTrue(contains(lines, "profile_receiver_major_hit"),
                "major receiver direct path is missing");
        Asserts.assertTrue(contains(lines, "profile_receiver_major_miss"),
                "major receiver dynamic fallback is missing");
        Asserts.assertTrue(contains(lines, "major_receiver_merge"),
                "major receiver results should merge");
        Asserts.assertTrue(blockContains(lines, "profile_receiver_major_miss:", "invoke hotspotcc"),
                "major receiver miss should contain a dynamic invoke");
        Asserts.assertFalse(blockContains(lines, "profile_receiver_major_miss:",
                        "llvm.experimental.deoptimize"),
                "major receiver miss must not deoptimize");
    }

    private static void checkInterfaceReceiverIR() throws Exception {
        List<String> lines = latestDumpLines("interfaceCallTarget");
        Asserts.assertTrue(contains(lines, "profile_receiver_hit"),
                "interface receiver direct path is missing");
        Asserts.assertTrue(contains(lines, "profile_receiver_miss"),
                "interface receiver guard miss is missing");
        Asserts.assertTrue(blockContains(lines, "profile_receiver_miss:",
                        "llvm.experimental.deoptimize"),
                "monomorphic interface miss should deoptimize");
    }

    private static void checkReceiverFallbackIR() throws Exception {
        List<String> lines = latestDumpLines("receiverFallbackTarget");
        Asserts.assertTrue(contains(lines, "profile_receiver_hit"),
                "throttled monomorphic call should retain its direct path");
        Asserts.assertTrue(contains(lines, "profile_receiver_miss"),
                "throttled monomorphic call should have a dynamic miss path");
        Asserts.assertTrue(contains(lines, "receiver_merge"),
                "direct and dynamic receiver results should merge");
        Asserts.assertTrue(blockContains(lines, "profile_receiver_miss:", "invoke hotspotcc"),
                "throttled monomorphic miss should contain a dynamic invoke");
        Asserts.assertFalse(blockContains(lines, "profile_receiver_miss:",
                        "llvm.experimental.deoptimize"),
                "throttled monomorphic miss must not deoptimize");
    }

    private static boolean contains(List<String> lines, String content) {
        for (String line : lines) {
            if (line.contains(content)) {
                return true;
            }
        }
        return false;
    }

    private static int bimorphicDeoptCount() {
        return WB.getDeoptCount("bimorphic", "maybe_recompile")
                + WB.getDeoptCount("bimorphic_or_optimized_type_check", "maybe_recompile");
    }

    private static boolean containsPattern(List<String> lines, String content) {
        Pattern pattern = Pattern.compile(content);
        for (String line : lines) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean blockContains(List<String> lines, String blockMarker, String content) {
        boolean inBlock = false;
        for (String line : lines) {
            if (!inBlock) {
                inBlock = line.contains(blockMarker);
                continue;
            }
            if (line.matches("[A-Za-z0-9_.$-]+:.*")) {
                return false;
            }
            if (line.contains(content)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> latestDumpLines(String methodName) throws Exception {
        String filePrefix = TestPGOBase.class.getName().replace('.', '_') + "_" + methodName;
        Path folder = Paths.get(System.getProperty("user.dir"));
        Path latest = Files.list(folder)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith(filePrefix)
                            && fileName.endsWith(".ll")
                            && !fileName.endsWith("_optimized.ll");
                })
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .orElseThrow(() -> new RuntimeException("No matched IR dump for " + methodName));

        return Files.readAllLines(latest)
                .stream()
                .map(str -> str.replaceAll("\\s+", " ").trim())
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }

    private static Method compile(String name, Class<?>... parameterTypes) throws Exception {
        return compile(TestPGOBase.class.getDeclaredMethod(name, parameterTypes));
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

    private static int branchTarget(int value) {
        if (value >= 0) {
            return value + 1;
        }
        return value - 2;
    }


    private static int profileFlagOffTarget(int value) {
        if (value >= 0) {
            return value + 1;
        }
        return value - 2;
    }

    private static int perBciRecompileTarget(int value) {
        if (value >= 0) {
            return value + 1;
        }
        return value - 2;
    }

    private static int staticBranchTarget(int value) {
        if (value == 7) {
            return value + 1;
        }
        return value - 1;
    }

    private static int useInterpreterGateTarget(int value) {
        if (value == 7) {
            return value + 1;
        }
        return value - 1;
    }

    private static int deoptResultBranchesTarget(int value, int divisor) {
        int local = value;
        int fallback = value + 7;
        try {
            if (local < 0) {
                local = ~local;
                fallback = ~fallback;
            }
            return 100 / divisor + local;
        } catch (ArithmeticException e) {
            return local + fallback;
        }
    }

    private static int wholeMethodTrapTarget(int first, int second) {
        int result = 0;
        if (first >= 0) {
            result += first + 1;
        } else {
            result += first - 2;
        }
        if (second >= 0) {
            result += second + 3;
        } else {
            result += second - 4;
        }
        return result;
    }

    private static int recompileLimitTarget(int first, int second) {
        int result = 0;
        if (first >= 0) {
            result += first + 1;
        } else {
            result += first - 2;
        }
        if (second >= 0) {
            result += second + 3;
        } else {
            result += second - 4;
        }
        return result;
    }

    private static int branchExpected(int value) {
        return value >= 0 ? value + 1 : value - 2;
    }

    private static int wholeMethodTrapExpected(int first, int second) {
        int result = first >= 0 ? first + 1 : first - 2;
        result += second >= 0 ? second + 3 : second - 4;
        return result;
    }

    private static int deoptResultBranchesExpected(int value, int divisor) {
        int local = value;
        int fallback = value + 7;
        try {
            if (local < 0) {
                local = ~local;
                fallback = ~fallback;
            }
            return 100 / divisor + local;
        } catch (ArithmeticException e) {
            return local + fallback;
        }
    }

    // --- Receiver profile devirtualization test ---

    static class Base {
        public int foo() { return 42; }
    }

    static class DerivedA extends Base {
        public int foo() { return 100; }
    }

    static class DerivedB extends Base {
        public int foo() { return 200; }
    }

    static class DerivedC extends Base {
        public int foo() { return 300; }
    }

    interface ReceiverInterface {
        int foo();
    }

    static class InterfaceA implements ReceiverInterface {
        public int foo() { return 400; }
    }

    static class InterfaceB implements ReceiverInterface {
        public int foo() { return 500; }
    }

    public static int virtualCallTarget(Base obj) {
        return obj.foo();
    }

    // Separate method so it gets its own MDO/bci distinct from virtualCallTarget.
    public static int bimorphicCallTarget(Base obj) {
        return obj.foo();
    }

    public static int majorReceiverCallTarget(Base obj) {
        return obj.foo();
    }

    public static int interfaceCallTarget(ReceiverInterface obj) {
        return obj.foo();
    }

    public static int receiverFallbackTarget(Base obj) {
        return obj.foo();
    }

    public static int receiverTypeProfileOffTarget(Base obj) {
        return obj.foo();
    }
}
