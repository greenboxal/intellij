/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.ideinfo;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import com.google.idea.blaze.base.model.primitives.Kind;
import com.google.idea.blaze.base.model.primitives.Label;

import javax.annotation.Nullable;
import java.util.Objects;

/** Ide info specific to proto rules. */
public final class ProtoIdeInfo implements ProtoWrapper<IntellijIdeInfo.ProtoIdeInfo> {
  private final ImmutableList<ArtifactLocation> sources;
  @Nullable private final String protoSourcePath;
  @Nullable private final ArtifactLocation descriptorSet;

  private ProtoIdeInfo(
      ImmutableList<ArtifactLocation> sources,
      @Nullable String protoSourcePath,
      @Nullable ArtifactLocation descriptorSet) {
    this.sources = sources;
    this.protoSourcePath = protoSourcePath;
    this.descriptorSet = descriptorSet;
  }

  public static ProtoIdeInfo fromProto(
      IntellijIdeInfo.ProtoIdeInfo proto, Label targetLabel, Kind targetKind) {
    return new ProtoIdeInfo(
        ProtoWrapper.map(proto.getSourcesList(), ArtifactLocation::fromProto),
        ImportPathReplacer.fixImportPath(
            Strings.emptyToNull(proto.getProtoSourceRoot()), targetLabel, targetKind),
            ArtifactLocation.fromProto(proto.getDescriptorSet()));
  }

  @Override
  public IntellijIdeInfo.ProtoIdeInfo toProto() {
    IntellijIdeInfo.ProtoIdeInfo.Builder builder = IntellijIdeInfo.ProtoIdeInfo.newBuilder();
    builder.addAllSources(ProtoWrapper.mapToProtos(sources));
    if (descriptorSet != null) {
      builder.setDescriptorSet(descriptorSet.toProto());
    }
    ProtoWrapper.setIfNotNull(builder::setProtoSourceRoot, protoSourcePath);
    return builder.build();
  }

  public ImmutableList<ArtifactLocation> getSources() {
    return sources;
  }

  @Nullable
  public String getProtoSourcePath() {
    return protoSourcePath;
  }

  @Nullable
  public ArtifactLocation getDescriptorSet() {
    return descriptorSet;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for go rule info */
  public static class Builder {
    private final ImmutableList.Builder<ArtifactLocation> sources = ImmutableList.builder();
    @Nullable private String protoSourcePath = null;
    @Nullable private ArtifactLocation descriptorSet = null;

    public Builder addSource(ArtifactLocation source) {
      this.sources.add(source);
      return this;
    }

    public Builder setProtoSourcePath(String protoSourcePath) {
      this.protoSourcePath = protoSourcePath;
      return this;
    }

    public Builder setDescriptorSet(ArtifactLocation descriptorSet) {
      this.descriptorSet = descriptorSet;
      return this;
    }

    public ProtoIdeInfo build() {
      return new ProtoIdeInfo(sources.build(), protoSourcePath, descriptorSet);
    }
  }

  @Override
  public String toString() {
    return "GoIdeInfo{"
        + "\n"
        + "  sources="
        + getSources()
        + "\n"
        + "  importPath="
        + getProtoSourcePath()
        + "\n"
        + "  libraryLabels="
        + getDescriptorSet()
        + "\n"
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProtoIdeInfo goIdeInfo = (ProtoIdeInfo) o;
    return Objects.equals(sources, goIdeInfo.sources)
        && Objects.equals(protoSourcePath, goIdeInfo.protoSourcePath)
        && Objects.equals(descriptorSet, goIdeInfo.descriptorSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sources, protoSourcePath, descriptorSet);
  }
}
