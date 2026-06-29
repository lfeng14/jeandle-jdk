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

#ifndef SHARE_JEANDLE_PROFILE_HPP
#define SHARE_JEANDLE_PROFILE_HPP

#include "jeandle/__hotspotHeadersBegin__.hpp"
#include "ci/ciCallProfile.hpp"
#include "ci/ciMethod.hpp"
#include "ci/ciMethodData.hpp"
#include "memory/allocation.hpp"
#include "runtime/deoptimization.hpp"

// Read-only wrapper around the method profile data used by Jeandle.
class JeandleProfile : public StackObj {
  // Match C2 branch profile gating in Parse::dynamic_branch_prediction.
  static const uint MinBranchProfileCount = 40;

  ciMethod* _method;
  ciMethodData* _mdo;

 public:
  explicit JeandleProfile(ciMethod* method);

  bool has_profile() const;
  bool is_mature() const;
  bool has_trap_at(int bci, Deoptimization::DeoptReason reason) const;
  bool has_too_many_traps(Deoptimization::DeoptReason reason) const;
  bool has_too_many_recompiles(int bci, Deoptimization::DeoptReason reason) const;
  bool should_use_branch_profile(int taken, int not_taken) const;
  bool should_speculate_branch(int bci, Deoptimization::DeoptReason reason, int taken, int not_taken) const;
  bool should_speculate_receiver(int bci, Deoptimization::DeoptReason reason) const;

  struct BranchCounts {
    int taken;
    int not_taken;
    bool valid;

    int64_t total() const { return (int64_t) taken + (int64_t) not_taken; }
  };

  struct SwitchCounts {
    uint default_count;
    int number_of_cases;
    bool valid;
  };

  struct ReceiverProfile {
    int morphism;
    ciKlass* receiver0;
    ciKlass* receiver1;
    uint count0;
    uint count1;
    uint site_count;
    bool has_major_receiver;
    bool valid;
  };

  int invocation_count() const;
  BranchCounts branch_at(int bci) const;
  SwitchCounts switch_at(int bci) const;
  uint switch_case_count_at(int bci, int index) const;
  ReceiverProfile receiver_at(int bci) const;
};

#endif // SHARE_JEANDLE_PROFILE_HPP
