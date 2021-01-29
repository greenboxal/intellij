package com.google.idea.blaze.protobuf.resolve;

import com.intellij.openapi.diagnostic.Logger;
import idea.plugin.protoeditor.lang.psi.PbTextFile;
import idea.plugin.protoeditor.lang.resolve.SchemaInfo;
import idea.plugin.protoeditor.lang.resolve.SchemaProvider;
import org.jetbrains.annotations.Nullable;

public class BazelSchemaProvider implements SchemaProvider {
    private static final Logger log = Logger.getInstance(BazelSchemaProvider.class);

    @Nullable
    @Override
    public SchemaInfo getSchemaInfo(PbTextFile file) {
        return null;
    }
}