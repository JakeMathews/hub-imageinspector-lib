/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDirLayerEntry implements LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveInputStream layerInputStream;
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public FileDirLayerEntry(final TarArchiveInputStream layerInputStream, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerInputStream = layerInputStream;
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public Optional<File> process() {
        final Optional<File> otherFileToDeleteNone = Optional.empty();
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing file/dir: %s", fileSystemEntryName));

        final File outputFile = new File(layerOutputDir, fileSystemEntryName);
        if (layerEntry.isFile()) {
            logger.trace(String.format("Processing file: %s", fileSystemEntryName));
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            logger.trace(String.format("Creating output stream for %s", outputFile.getAbsolutePath()));
            OutputStream outputFileStream = null;
            try {
                outputFileStream = new FileOutputStream(outputFile);
            } catch (final FileNotFoundException e1) {
                logger.error(String.format("Error creating output stream for %s", outputFile.getAbsolutePath()), e1);
                return otherFileToDeleteNone;
            }
            try {
                IOUtils.copy(layerInputStream, outputFileStream);
            } catch (final IOException e) {
                logger.error(String.format("Error copying file %s to %s: %s", fileSystemEntryName, outputFile.getAbsolutePath(), e.getMessage()));
                return otherFileToDeleteNone;
            } finally {
                if (outputFileStream != null) {
                    try {
                        outputFileStream.close();
                    } catch (final IOException e) {
                        logger.error(String.format("Error closing output file stream for: %s: %s", outputFile.getAbsolutePath(), e.getMessage()));
                    }
                }
            }
        } else {
            final boolean mkdirSucceeded = outputFile.mkdirs();
            if (!mkdirSucceeded) {
                logger.trace(String.format("mkdir of %s didn't succeed, but it might have already existed", outputFile.getAbsolutePath()));
            }
        }
        return otherFileToDeleteNone;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
