package com.synopsys.integration.blackduck.imageinspector.linux.extractor.composed;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.hub.bdio.model.Forge;

public class NullExtractorBehavior implements ExtractorBehavior {
    private final static List<Forge> defaultForges = new ArrayList<>(0);

    @Override
    public PkgMgrExecutor getPkgMgrExecutor() {
        return null;
    }

    @Override
    public PackageManagerEnum getPackageManagerEnum() {
        return PackageManagerEnum.NULL;
    }

    @Override
    public List<Forge> getDefaultForges() {
        return defaultForges;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final String architecture, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String preferredAliasNamespace) {
        return new ArrayList<>();
    }
}