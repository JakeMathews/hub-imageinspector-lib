/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.extractor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.executor.RpmExecutor;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;

@Component
public class RpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RpmExecutor executor;

    @Override
    @PostConstruct
    public void init() {
        final List<Forge> forges = new ArrayList<>();
        forges.add(OperatingSystemEnum.CENTOS.getForge());
        forges.add(OperatingSystemEnum.FEDORA.getForge());
        forges.add(OperatingSystemEnum.REDHAT.getForge());
        initValues(PackageManagerEnum.RPM, executor, forges);
    }

    private boolean valid(final String packageLine) {
        return packageLine.matches(".+-.+-.+\\..*");
    }

    @Override
    public void extractComponents(final MutableDependencyGraph dependencies, final String dockerImageRepo, final String dockerImageTag, final String architecture, final String[] packageList, final String extractedForgeName) {
        logger.debug("extractComponents: Received ${packageList.length} package lines");
        for (final String packageLine : packageList) {
            if (valid(packageLine)) {
                final int lastDotIndex = packageLine.lastIndexOf('.');
                final String arch = packageLine.substring(lastDotIndex + 1);
                final int lastDashIndex = packageLine.lastIndexOf('-');
                final String nameVersion = packageLine.substring(0, lastDashIndex);
                final int secondToLastDashIndex = nameVersion.lastIndexOf('-');
                final String versionRelease = packageLine.substring(secondToLastDashIndex + 1, lastDotIndex);
                final String artifact = packageLine.substring(0, secondToLastDashIndex);
                final String externalId = String.format("%s/%s/%s", artifact, versionRelease, arch);
                logger.debug(String.format("Adding externalId %s to components list", externalId));
                createBdioComponent(dependencies, artifact, versionRelease, externalId, arch, extractedForgeName);
            }
        }
    }
}
