/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.android.sync.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.intellij.model.ProjectData;
import com.google.idea.blaze.android.libraries.UnpackedAars;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.LibraryArtifact;
import com.google.idea.blaze.base.model.BlazeLibrary;
import com.google.idea.blaze.base.model.LibraryKey;
import com.google.idea.blaze.base.sync.workspace.ArtifactLocationDecoder;
import com.google.idea.blaze.java.libraries.JarCache;
import com.google.idea.common.experiments.FeatureRolloutExperiment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library.ModifiableModel;
import com.intellij.openapi.util.text.StringUtil;
import java.io.File;
import javax.annotation.concurrent.Immutable;
import org.jetbrains.annotations.Nullable;

/**
 * A library corresponding to an AAR file. Has jars and resource directories.
 *
 * <p>AAR Libraries are structured to generate an IntelliJ Library that looks like a library that
 * Studio will generate for an aar in the gradle world. In particular, the classes.jar and res
 * folders are all included as source roots in a single library. One consequence of this is that we
 * don't end up using the blaze sync plugin's handling of jars (which for instance only attaches
 * sources on demand).
 */
@Immutable
public final class AarLibrary extends BlazeLibrary {
  private static final Logger logger = Logger.getInstance(AarLibrary.class);

  @VisibleForTesting
  public static final FeatureRolloutExperiment exportResourcePackage =
      new FeatureRolloutExperiment("aswb.aarlibrary.expose.res.package");

  // libraryArtifact would be null if this aar is created by aspect file. Such aar is generated for
  // generated resources which should not have any bundled jar file.
  @Nullable public final LibraryArtifact libraryArtifact;
  public final ArtifactLocation aarArtifact;
  @Nullable public final String resourcePackage;

  public AarLibrary(ArtifactLocation aarArtifact) {
    this(null, aarArtifact, null);
  }

  public AarLibrary(ArtifactLocation artifactLocation, @Nullable String resourcePackage) {
    this(null, artifactLocation, resourcePackage);
  }

  public AarLibrary(
      @Nullable LibraryArtifact libraryArtifact,
      ArtifactLocation aarArtifact,
      @Nullable String resourcePackage) {
    // Use the aar's name for the library key. The jar name is the same for all AARs, so could more
    // easily get a hash collision.
    super(LibraryKey.fromArtifactLocation(aarArtifact));
    this.libraryArtifact = libraryArtifact;
    this.aarArtifact = aarArtifact;
    if (exportResourcePackage.isEnabled()) {
      this.resourcePackage = resourcePackage;
    } else {
      this.resourcePackage = null;
    }
  }

  static AarLibrary fromProto(ProjectData.BlazeLibrary proto) {
    ProjectData.AarLibrary aarLibrary = proto.getAarLibrary();
    return new AarLibrary(
        aarLibrary.hasLibraryArtifact()
            ? LibraryArtifact.fromProto(aarLibrary.getLibraryArtifact())
            : null,
        ArtifactLocation.fromProto(aarLibrary.getAarArtifact()),
        aarLibrary.getResourcePackage());
  }

  @Override
  public ProjectData.BlazeLibrary toProto() {
    ProjectData.AarLibrary.Builder aarLibraryBuilder =
        ProjectData.AarLibrary.newBuilder().setAarArtifact(aarArtifact.toProto());
    if (libraryArtifact != null) {
      aarLibraryBuilder.setLibraryArtifact(libraryArtifact.toProto());
    }

    if (!StringUtil.isEmpty(resourcePackage)) {
      aarLibraryBuilder.setResourcePackage(resourcePackage);
    }
    return super.toProto().toBuilder().setAarLibrary(aarLibraryBuilder.build()).build();
  }

  /**
   * Create an IntelliJ library that matches Android Studio's expectation for an AAR. See {@link
   * org.jetbrains.android.facet.ResourceFolderManager#addAarsFromModuleLibraries}.
   */
  @Override
  public void modifyLibraryModel(
      Project project,
      ArtifactLocationDecoder artifactLocationDecoder,
      ModifiableModel libraryModel) {
    UnpackedAars unpackedAars = UnpackedAars.getInstance(project);

    File resourceDirectory = unpackedAars.getResourceDirectory(artifactLocationDecoder, this);
    if (resourceDirectory == null) {
      logger.warn("No resource directory found for aar: " + aarArtifact);
      return;
    }
    libraryModel.addRoot(pathToUrl(resourceDirectory), OrderRootType.CLASSES);

    // aars that were generated by the aspect to expose resources external to the project don't
    // have class jars or sources
    if (libraryArtifact == null) {
      return;
    }

    File jar = unpackedAars.getClassJar(artifactLocationDecoder, this);
    if (jar != null) {
      libraryModel.addRoot(pathToUrl(jar), OrderRootType.CLASSES);
    }

    // Unconditionally add any linked to source jars. BlazeJarLibrary doesn't do this - it only
    // attaches sources for libraries that the user explicitly asks for. We don't do that for two
    // reasons: 1) all the logic for attaching sources to a library (AttachSourceJarAction,
    // BlazeSourceJarNavigationPolicy, LibraryActionHelper etc) are all tied to Java specific
    // libraries, and 2) So far, aar_imports are primarily used for very few 3rd party dependencies.
    // Longer term, we may want to make this behave just like the Java libraries.
    for (ArtifactLocation srcJar : libraryArtifact.getSourceJars()) {
      File sourceJar =
          JarCache.getInstance(project).getCachedSourceJar(artifactLocationDecoder, srcJar);
      if (sourceJar != null) {
        libraryModel.addRoot(pathToUrl(sourceJar), OrderRootType.SOURCES);
      }
    }
  }
}
