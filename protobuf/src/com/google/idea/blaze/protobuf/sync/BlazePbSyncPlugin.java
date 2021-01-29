/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.protobuf.sync;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.LanguageClass;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.model.primitives.WorkspaceType;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.sync.BlazeSyncPlugin;
import com.google.idea.blaze.base.sync.GenericSourceFolderProvider;
import com.google.idea.blaze.base.sync.SourceFolderProvider;
import com.google.idea.blaze.base.sync.libraries.LibrarySource;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Supports golang. */
public class BlazePbSyncPlugin implements BlazeSyncPlugin {

  static final ImmutableSet<String> PB_LIBRARY_PREFIXES = ImmutableSet.of();

  @Override
  public Set<LanguageClass> getSupportedLanguagesInWorkspace(WorkspaceType workspaceType) {
    return ImmutableSet.of(LanguageClass.PROTO);
  }

  @Override
  public ImmutableList<WorkspaceType> getSupportedWorkspaceTypes() {
    return ImmutableList.of();
  }

  @Nullable
  @Override
  public SourceFolderProvider getSourceFolderProvider(BlazeProjectData projectData) {
    return GenericSourceFolderProvider.INSTANCE;
  }

  @Override
  public void updateProjectStructure(
      Project project,
      BlazeContext context,
      WorkspaceRoot workspaceRoot,
      ProjectViewSet projectViewSet,
      BlazeProjectData blazeProjectData,
      @Nullable BlazeProjectData oldBlazeProjectData,
      ModuleEditor moduleEditor,
      Module workspaceModule,
      ModifiableRootModel workspaceModifiableModel) {
    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.PROTO)) {
      return;
    }
    for (Library lib : getPbLibraries(project)) {
      if (workspaceModifiableModel.findLibraryOrderEntry(lib) == null) {
        workspaceModifiableModel.addLibraryEntry(lib);
      }
    }
  }

  private static List<Library> getPbLibraries(Project project) {
    List<Library> libraries = Lists.newArrayList();
    LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
    for (Library lib : registrar.getLibraryTable().getLibraries()) {
      if (BlazePbLibrarySource.isPbLibrary(lib)) {
        libraries.add(lib);
      }
    }

    for (Library lib : registrar.getLibraryTable(project).getLibraries()) {
      if (BlazePbLibrarySource.isPbLibrary(lib)) {
        libraries.add(lib);
      }
    }
    return libraries;
  }

  @Nullable
  @Override
  public LibrarySource getLibrarySource(
      ProjectViewSet projectViewSet, BlazeProjectData blazeProjectData) {
    if (!blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.PROTO)) {
      return null;
    }
    return BlazePbLibrarySource.INSTANCE;
  }

  @Override
  public boolean refreshExecutionRoot(BlazeProjectData blazeProjectData) {
    return blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.PROTO);
  }
}
