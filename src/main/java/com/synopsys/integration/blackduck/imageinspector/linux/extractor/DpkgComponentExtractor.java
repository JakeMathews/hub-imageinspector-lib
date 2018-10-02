package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.PkgMgrExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class DpkgComponentExtractor implements ComponentExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR = "[  ]+";
    private static final String PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST = "\\+\\+\\+-=+-=+-=+-=+";
    private final PkgMgrExecutor pkgMgrExecutor;

    public DpkgComponentExtractor(final PkgMgrExecutor pkgMgrExecutor) {
        this.pkgMgrExecutor = pkgMgrExecutor;
    }

    @Override
    public List<ComponentDetails> extractComponents(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgrDatabase imagePkgMgrDatabase, final String linuxDistroName)
            throws IntegrationException {
        final List<ComponentDetails> components = new ArrayList<>();
        final String[] packageList = pkgMgrExecutor.runPackageManager(imagePkgMgrDatabase);
        boolean startOfComponents = false;
        for (final String packageLine : packageList) {

            if (packageLine != null) {
                if (packageLine.matches(PATTERN_FOR_LINE_PRECEDING_COMPONENT_LIST)) {
                    startOfComponents = true;
                } else if (startOfComponents) {
                    // Expect: statusChar name version arch
                    // Or: statusChar name:arch version arch
                    final char packageStatus = packageLine.charAt(1);
                    if (isInstalledStatus(packageStatus)) {
                        final String componentInfo = packageLine.substring(3);
                        final String[] componentInfoParts = componentInfo.trim().split(PATTERN_FOR_COMPONENT_DETAILS_SEPARATOR);
                        String name = componentInfoParts[0];
                        final String version = componentInfoParts[1];
                        final String archFromPkgMgrOutput = componentInfoParts[2];
                        if (name.contains(":")) {
                            name = name.substring(0, name.indexOf(":"));
                        }
                        final String externalId = String.format(EXTERNAL_ID_STRING_FORMAT, name, version, archFromPkgMgrOutput);
                        logger.trace(String.format("Constructed externalId: %s", externalId));
                        components.add(new ComponentDetails(name, version, externalId, archFromPkgMgrOutput, linuxDistroName));
                    } else {
                        logger.trace(String.format("Package \"%s\" is listed but not installed (package status: %s)", packageLine, packageStatus));
                    }
                }
            }
        }
        return components;
    }

    private boolean isInstalledStatus(final Character packageStatus) {
        if (packageStatus == 'i' || packageStatus == 'W' || packageStatus == 't') {
            return true;
        }
        return false;
    }
}
