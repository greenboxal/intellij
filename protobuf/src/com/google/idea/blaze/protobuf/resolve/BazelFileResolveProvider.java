package com.google.idea.blaze.protobuf.resolve;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.sync.SyncCache;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import idea.plugin.protoeditor.lang.resolve.FileResolveProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class BazelFileResolveProvider implements FileResolveProvider {
    private static final Logger log = Logger.getInstance(BazelFileResolveProvider.class);

    private static final String PROTO_PACKAGE_MAP_KEY = "BlazeProtoPackageMap";
    private static final String PROTO_TARGET_MAP_KEY = "BlazeProtoTargetMap";
    private static final String DESCRIPTOR = "google/protobuf/descriptor.proto";

    @Nullable
    @Override
    public VirtualFile findFile(@NotNull String path, @NotNull Project project) {
        return doResolve(path, project);
    }

    @NotNull
    @Override
    public Collection<ChildEntry> getChildEntries(@NotNull String path, @NotNull Project project) {
        VirtualFile root = doResolve(path, project);
        return findChildEntriesInRoots(path, new VirtualFile[] { root });
    }

    @NotNull
    @Override
    public Collection<ChildEntry> getChildEntries(@NotNull String path, @NotNull Module module) {
        log.warn(path);
        VirtualFile[] roots = ModuleRootManager.getInstance(module).orderEntries().getAllLibrariesAndSdkClassesRoots();
        return findChildEntriesInRoots(path, roots);
    }

    @Nullable
    @Override
    public VirtualFile getDescriptorFile(@NotNull Project project) {
        return findFile(DESCRIPTOR, project);
    }

    @Nullable
    @Override
    public VirtualFile getDescriptorFile(@NotNull Module module) {
        return findFile(DESCRIPTOR, module);
    }

    @NotNull
    @Override
    public GlobalSearchScope getSearchScope(@NotNull Project project) {
        return GlobalSearchScope.everythingScope(project);
    }

    private static Map<String, BazelProtoSource> getProtoTargetMap(Project project) {
        return SyncCache.getInstance(project).get(PROTO_TARGET_MAP_KEY, BazelFileResolveProvider::buildProtoTargetMap);
    }

    private static ImmutableMap<String, BazelProtoSource> buildProtoTargetMap(Project p, BlazeProjectData projectData) {
        log.warn("buildProtoTargetMap");

        return projectData
                .getTargetMap()
                .targets()
                .stream()
                .peek(x -> log.warn("Found target " + x.getKey()))
                .filter(t -> t.getProtoIdeInfo() != null)
                .flatMap(BazelProtoSource::getSources)
                .peek(x -> log.warn("Found source " + x.getArtifactLocation().getRelativePath() + " -> " + x.getImportPath()))
                .collect(
                        ImmutableMap.toImmutableMap(
                                BazelProtoSource::getImportPath,
                                x -> x,
                                // duplicates are possible (e.g., same target with different aspects)
                                // choose the one with the most sources (though they're probably the same)
                                (first, second) ->
                                        first.getProtoIdeInfo().getSources().size()
                                                >= second.getProtoIdeInfo().getSources().size()
                                                ? first
                                                : second));
    }

    static VirtualFile doResolve(String importPath, Project project) {
        log.warn("Resolving " + importPath);

        BlazeProjectData projectData =
                BlazeProjectDataManager.getInstance(project).getBlazeProjectData();
        if (projectData == null) {
            return null;
        }
        Map<String, BazelProtoSource> protoTargetMap = Preconditions.checkNotNull(getProtoTargetMap(project));
        BazelProtoSource source = protoTargetMap.get(importPath);
        if (source == null) {
            return null;
        }
        File file = projectData.getArtifactLocationDecoder().resolveSource(source.getArtifactLocation());
        if (file == null) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    private Collection<ChildEntry> findChildEntriesInRoots(String path, VirtualFile[] roots) {
        return Arrays.stream(roots).map(root -> root.findChild(path))
                .filter(Objects::nonNull)
                .flatMap(root -> VfsUtil.getChildren(root, PROTO_AND_DIRECTORY_FILTER)
                        .stream()
                        .map(child -> new ChildEntry(child.getName(), child.isDirectory()))).collect(Collectors.toList());
    }
}