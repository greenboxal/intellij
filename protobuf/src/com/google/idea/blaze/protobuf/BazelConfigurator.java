package com.google.idea.blaze.protobuf;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import idea.plugin.protoeditor.ide.settings.PbProjectSettings;
import idea.plugin.protoeditor.ide.settings.ProjectSettingsConfigurator;
import org.jetbrains.annotations.Nullable;

public class BazelConfigurator implements ProjectSettingsConfigurator {
    private static final Logger log = Logger.getInstance(BazelConfigurator.class);

    @Nullable
    @Override
    public PbProjectSettings configure(Project project, PbProjectSettings settings) {
        log.warn("ProjectSettingsConfigurator:configure");

        return null;
    }
}