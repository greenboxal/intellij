package com.google.idea.blaze.protobuf.resolve;

import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.ProtoIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetKey;
import com.intellij.openapi.diagnostic.Logger;

import java.util.stream.Stream;

public final class BazelProtoSource {
    private static final Logger log = Logger.getInstance(BazelProtoSource.class);

    private final TargetIdeInfo targetIdeInfo;
    private final ProtoIdeInfo protoIdeInfo;
    private final ArtifactLocation artifactLocation;
    private final String importPath;

    public BazelProtoSource(TargetIdeInfo targetIdeInfo, ProtoIdeInfo protoIdeInfo, ArtifactLocation artifactLocation) {
        String importPath = artifactLocation.getRelativePath();
        String importRoot = protoIdeInfo.getProtoSourcePath();

        if (importRoot != null && importPath.startsWith(importRoot)) {
            importPath = importPath.substring(importRoot.length());
        }

        this.targetIdeInfo = targetIdeInfo;
        this.protoIdeInfo = protoIdeInfo;
        this.artifactLocation = artifactLocation;
        this.importPath = importPath;
    }

    public static Stream<BazelProtoSource> getSources(TargetIdeInfo info) {
        ProtoIdeInfo proto = info.getProtoIdeInfo();

        if (proto == null) {
            return Stream.empty();
        }

        log.warn("Getting sources for " + info.getKey());

        return proto
                .getSources()
                .stream()
                .map(s -> new BazelProtoSource(info, proto, s));
    }

    public TargetIdeInfo getTargetIdeInfo() {
        return targetIdeInfo;
    }

    public ArtifactLocation getArtifactLocation() {
        return artifactLocation;
    }

    public ProtoIdeInfo getProtoIdeInfo() {
        return protoIdeInfo;
    }

    public String getImportPath() {
        return importPath;
    }

    public TargetKey getKey() {
        return targetIdeInfo.getKey();
    }
}