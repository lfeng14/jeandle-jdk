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

#include "jeandle/jeandleProfile.hpp"

#include "jeandle/jeandle_globals.hpp"

#include "jeandle/__hotspotHeadersBegin__.hpp"
#include "oops/methodData.hpp"
#include "runtime/globals.hpp"

static ciProfileData* profile_data_at(ciMethodData* mdo, int bci) {
  if (mdo == nullptr || mdo->is_empty()) {
    return nullptr;
  }

  // Use the regular per-bci ProfileData. Passing a method here would look in
  // the speculative-trap extra-data area instead.
  return mdo->bci_to_data(bci, nullptr);
}

static MultiBranchData* multi_branch_data_at(ciMethodData* mdo, int bci) {
  ciProfileData* data = profile_data_at(mdo, bci);
  return data != nullptr && data->is_MultiBranchData() ? data->as_MultiBranchData() : nullptr;
}

static int scaled_count(ciMethod* method, uint count) {
  if (count > (uint) max_jint) {
    return -1;
  }
  return method != nullptr ? method->scale_count((int) count) : (int) count;
}

JeandleProfile::JeandleProfile(ciMethod* method)
  : _method(method),
    _mdo((JeandleUseProfile && method != nullptr) ? method->method_data() : nullptr) {}

bool JeandleProfile::has_profile() const {
  return _mdo != nullptr && !_mdo->is_empty();
}

bool JeandleProfile::is_mature() const {
  return has_profile() && _mdo->is_mature();
}

bool JeandleProfile::has_trap_at(int bci, Deoptimization::DeoptReason reason) const {
  if (_mdo == nullptr) {
    return false;
  }
  // Treat the conservative "maybe trapped here" answer as a real trap for
  // speculation gating. Metadata-only uses such as branch_weights do not need
  // this guard; uncommon-trap/speculative transforms do.
  return _mdo->has_trap_at(bci, nullptr, reason) != 0;
}

bool JeandleProfile::has_too_many_traps(Deoptimization::DeoptReason reason) const {
  if (_mdo == nullptr || _mdo->is_empty()) {
    return false;
  }
  return _mdo->trap_count(reason) >= Deoptimization::per_method_trap_limit(reason);
}

bool JeandleProfile::has_too_many_recompiles(int bci, Deoptimization::DeoptReason reason) const {
  if (_mdo == nullptr || _mdo->is_empty()) {
    return false;
  }

  uint bc_cutoff = (uint) PerBytecodeRecompilationCutoff / 8;
  uint method_cutoff = (uint) PerMethodRecompilationCutoff / 2 + 1;
  Deoptimization::DeoptReason per_bc_reason = Deoptimization::reason_recorded_per_bytecode_if_any(reason);
  ciMethod* trap_method = Deoptimization::reason_is_speculate(reason) ? _method : nullptr;

  if ((per_bc_reason == Deoptimization::Reason_none || _mdo->has_trap_at(bci, trap_method, reason) != 0) &&
      _mdo->trap_recompiled_at(bci, trap_method) &&
      _mdo->overflow_recompile_count() >= bc_cutoff) {
    return true;
  }

  return _mdo->trap_count(reason) != 0 && _mdo->decompile_count() >= method_cutoff;
}

bool JeandleProfile::should_use_branch_profile(int taken, int not_taken) const {
  if (!is_mature() || taken < 0 || not_taken < 0) {
    return false;
  }

  int64_t total = (int64_t) taken + (int64_t) not_taken;
  return total <= (int64_t) max_jint && total >= MinBranchProfileCount;
}

bool JeandleProfile::should_speculate_branch(int bci,
                                             Deoptimization::DeoptReason reason,
                                             int taken,
                                             int not_taken) const {
  return should_use_branch_profile(taken, not_taken) &&
         !has_trap_at(bci, reason) &&
         !has_too_many_traps(reason) &&
         !has_too_many_recompiles(bci, reason);
}

int JeandleProfile::invocation_count() const {
  return has_profile() ? _mdo->invocation_count() : 0;
}

JeandleProfile::BranchCounts JeandleProfile::branch_at(int bci) const {
  BranchCounts result = {0, 0, false};
  ciProfileData* data = profile_data_at(_mdo, bci);
  if (data == nullptr || !data->is_BranchData()) {
    return result;
  }

  BranchData* branch = data->as_BranchData();
  result.taken     = scaled_count(_method, branch->taken());
  result.not_taken = scaled_count(_method, branch->not_taken());
  result.valid     = true;
  return result;
}

JeandleProfile::SwitchCounts JeandleProfile::switch_at(int bci) const {
  SwitchCounts result = {0, 0, false};
  MultiBranchData* multi_branch = multi_branch_data_at(_mdo, bci);
  if (multi_branch == nullptr) {
    return result;
  }

  result.default_count = multi_branch->default_count();
  result.number_of_cases = multi_branch->number_of_cases();
  result.valid = true;
  return result;
}

uint JeandleProfile::switch_case_count_at(int bci, int index) const {
  MultiBranchData* multi_branch = multi_branch_data_at(_mdo, bci);
  if (multi_branch == nullptr) {
    return 0;
  }

  if (index < 0 || index >= multi_branch->number_of_cases()) {
    return 0;
  }
  return multi_branch->count_at(index);
}

JeandleProfile::ReceiverProfile JeandleProfile::monomorphic_receiver_at(int bci) const {
  ReceiverProfile result = {nullptr, 0, 0, false};
  if (_method == nullptr || !is_mature()) {
    return result;
  }

  ciCallProfile profile = _method->call_profile_at_bci(bci);
  if (profile.morphism() != 1 || !profile.has_receiver(0) || profile.receiver_count(0) <= 0) {
    return result;
  }

  ciKlass* receiver = profile.receiver(0);
  if (receiver == nullptr || !receiver->is_loaded()) {
    return result;
  }

  uint receiver_count = (uint) profile.receiver_count(0);
  uint site_count = profile.count() > 0 ? (uint) profile.count() : receiver_count;
  result.receiver_klass = receiver;
  result.receiver_count = receiver_count;
  result.site_count = site_count;
  result.valid = true;
  return result;
}
