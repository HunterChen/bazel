// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.cpp;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.util.Preconditions;

/**
 * Provides generic functionality for collecting the .dwo artifacts produced by any target
 * that compiles C++ files. Supports both transitive and "only direct outputs" collection.
 * Provides accessors for both PIC and non-PIC compilation modes.
 */
public class DwoArtifactsCollector {

  /**
   * The .dwo files collected by this target in non-PIC compilation mode (i.e. myobject.dwo).
   */
  private final NestedSet<Artifact> dwoArtifacts;

  /**
   * The .dwo files collected by this target in PIC compilation mode (i.e. myobject.pic.dwo).
   */
  private final NestedSet<Artifact> picDwoArtifacts;

  /**
   * Instantiates a "real" collector on meaningful data.
   */
  private DwoArtifactsCollector(CcCompilationOutputs compilationOutputs,
        Iterable<TransitiveInfoCollection> deps) {

    Preconditions.checkNotNull(compilationOutputs);
    Preconditions.checkNotNull(deps);

    // Note: .dwo collection works fine with any order, but tests may assume a
    // specific order for readability / simplicity purposes. See
    // DebugInfoPackagingTest for details.
    NestedSetBuilder<Artifact> dwoBuilder = NestedSetBuilder.compileOrder();
    NestedSetBuilder<Artifact> picDwoBuilder = NestedSetBuilder.compileOrder();

    dwoBuilder.addAll(compilationOutputs.getDwoFiles());
    picDwoBuilder.addAll(compilationOutputs.getPicDwoFiles());

    for (TransitiveInfoCollection info : deps) {
      CppDebugFileProvider provider = info.getProvider(CppDebugFileProvider.class);
      if (provider != null) {
        dwoBuilder.addTransitive(provider.getTransitiveDwoFiles());
        picDwoBuilder.addTransitive(provider.getTransitivePicDwoFiles());
      }
    }

    dwoArtifacts = dwoBuilder.build();
    picDwoArtifacts = picDwoBuilder.build();
  }

  /**
   * Instantiates an empty collector.
   */
  private DwoArtifactsCollector() {
    dwoArtifacts = NestedSetBuilder.<Artifact>emptySet(Order.COMPILE_ORDER);
    picDwoArtifacts = NestedSetBuilder.<Artifact>emptySet(Order.COMPILE_ORDER);
  }

  /**
   * Returns a new instance that collects direct outputs and transitive dependencies.
   *
   * @param compilationOutputs the output compilation context for the owning target
   * @param deps which of the target's transitive info collections should be visited
   */
  public static DwoArtifactsCollector transitiveCollector(CcCompilationOutputs compilationOutputs,
      Iterable<TransitiveInfoCollection> deps) {
    return new DwoArtifactsCollector(compilationOutputs, deps);
  }

  /**
   * Returns a new instance that collects direct outputs only.
   *
   * @param compilationOutputs the output compilation context for the owning target
   */
  public static DwoArtifactsCollector directCollector(CcCompilationOutputs compilationOutputs) {
      return new DwoArtifactsCollector(
          compilationOutputs, ImmutableList.<TransitiveInfoCollection>of());
  }

  /**
   * Returns a new instance that doesn't collect anything (its artifact sets are empty).
   */
  public static DwoArtifactsCollector emptyCollector() {
    return new DwoArtifactsCollector();
  }

  /**
   * Returns the .dwo files applicable to non-PIC compilation mode (i.e. myobject.dwo).
   */
  public NestedSet<Artifact> getDwoArtifacts() {
    return dwoArtifacts;
  }

  /**
   * Returns the .dwo files applicable to PIC compilation mode (i.e. myobject.pic.dwo).
   */
  public NestedSet<Artifact> getPicDwoArtifacts() {
    return picDwoArtifacts;
  }
}
