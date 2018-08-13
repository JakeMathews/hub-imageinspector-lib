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
package com.synopsys.integration.blackduck.imageinspector.linux;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;

@Component
public class Os {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OperatingSystemEnum deriveCurrentOs(final String currentLinuxDistro) throws IntegrationException {
        OperatingSystemEnum osEnum = OperatingSystemEnum.determineOperatingSystem(currentLinuxDistro);
        if (osEnum != null) {
            logger.debug(String.format("Using given value for current OS: %s", osEnum.toString()));
            return osEnum;
        }
        final String systemPropertyOsValue = System.getProperty("os.name");
        logger.debug(String.format("Deriving current OS; System.getProperty(\"os.name\") says: %s", systemPropertyOsValue));
        if (!isLinuxUnix(systemPropertyOsValue)) {
            throw new IntegrationException(String.format("System property OS value is '%s'; this appears to be a non-Linux/Unix system", systemPropertyOsValue));
        }
        final File rootDir = new File("/");
        final FileSys rootFileSys = new FileSys(rootDir);
        final Set<PackageManagerEnum> packageManagers = rootFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getInspectorOperatingSystem();
            logger.debug(String.format("Current Operating System %s", osEnum.name()));
            return osEnum;
        }
        throw new IntegrationException(String.format("Unable to determine current operating system; %d package managers found: %s", packageManagers.size(), packageManagers));
    }

    public void logMemory() {
        final Long total = Runtime.getRuntime().totalMemory();
        final Long free = Runtime.getRuntime().freeMemory();
        logger.debug(String.format("Heap: total: %d; free: %d", total, free));
    }

    private boolean isLinuxUnix(final String osName) {
        if (osName == null) {
            return false;
        }
        return osName.contains("nux") || osName.contains("nix") || osName.contains("aix");
    }
}