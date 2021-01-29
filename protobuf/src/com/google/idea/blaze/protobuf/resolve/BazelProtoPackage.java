package com.google.idea.blaze.protobuf.resolve;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.command.buildresult.OutputArtifactResolver;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.ProtoIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.Label;
import com.intellij.openapi.project.Project;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

public class BazelProtoPackage {

    public static ImmutableMultimap<Label, File> getUncachedTargetToFileMap(
            Project project, BlazeProjectData projectData) {
        ImmutableMultimap.Builder<Label, File> builder = ImmutableMultimap.builder();
        for (TargetIdeInfo target : projectData.getTargetMap().targets()) {
            if (target.getProtoIdeInfo() == null) {
                continue;
            }
            builder.putAll(
                    target.getKey().getLabel(),
                    getSourceFiles(target, project, projectData));
        }
        return builder.build();
    }

    private static ImmutableSet<File> getSourceFiles(
            TargetIdeInfo target,
            Project project,
            BlazeProjectData projectData) {
        return Stream.of(target.getProtoIdeInfo())
                .map(ProtoIdeInfo::getSources)
                .flatMap(Collection::stream)
                .map(a -> resolveArtifact(project, projectData, a))
                .filter(Objects::nonNull)
                .collect(toImmutableSet());
    }

    @Nullable
    private static File resolveArtifact(
            Project project, BlazeProjectData data, ArtifactLocation artifact) {
        return OutputArtifactResolver.resolve(project, data.getArtifactLocationDecoder(), artifact);
    }
}
