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
package com.blackducksoftware.integration.hub.imageinspector.linux.executor;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerFiles;

public abstract class PkgMgrExecutor extends Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Long CMD_TIMEOUT = 120000L;
    private final ReentrantLock lock = new ReentrantLock();
    private String upgradeCommand;
    private String listPackagesCommand;

    public abstract void init();

    void initValues(final String upgradeCommand, final String listPackagesCommand) {
        this.upgradeCommand = upgradeCommand;
        this.listPackagesCommand = listPackagesCommand;
    }

    public String[] runPackageManager(final ImagePkgMgr imagePkgMgr) throws IntegrationException, IOException, InterruptedException {
        logger.info("Requesting lock for package manager execution");
        lock.lock();
        logger.info("Acquired lock for package manager execution");
        try {
            final PackageManagerFiles pkgMgrFiles = new PackageManagerFiles();
            pkgMgrFiles.stubPackageManagerFiles(imagePkgMgr);
            final String[] packages = listPackages();
            logger.trace(String.format("Package count: %d", packages.length));
            return packages;
        } finally {
            logger.info("Finished package manager execution");
            lock.unlock();
            logger.info("Released lock after package manager execution");
        }
    }

    private String[] listPackages() throws IntegrationException, IOException, InterruptedException {
        String[] results;
        logger.debug("Executing package manager");
        try {
            results = executeCommand(listPackagesCommand, CMD_TIMEOUT);
            logger.info(String.format("Command %s executed successfully", listPackagesCommand));
        } catch (final Exception e) {
            if (!StringUtils.isBlank(upgradeCommand)) {
                logger.warn(String.format("Error executing \"%s\": %s; Trying to upgrade package database by executing: %s", listPackagesCommand, e.getMessage(), upgradeCommand));
                executeCommand(upgradeCommand, CMD_TIMEOUT);
                results = executeCommand(listPackagesCommand, CMD_TIMEOUT);
                logger.info(String.format("Command %s executed successfully on 2nd attempt (after db upgrade)", listPackagesCommand));
            } else {
                logger.error(String.format("Error executing \"%s\": %s; No upgrade command has been provided for this package manager", listPackagesCommand, e.getMessage()));
                throw e;
            }
        }
        logger.debug(String.format("Package manager reported %s package lines", results.length));
        return results;
    }

}
