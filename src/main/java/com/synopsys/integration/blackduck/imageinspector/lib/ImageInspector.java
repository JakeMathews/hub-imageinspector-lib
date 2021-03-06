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
package com.synopsys.integration.blackduck.imageinspector.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.PackageManagerEnum;
import com.synopsys.integration.blackduck.imageinspector.api.WrongInspectorOsException;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.DockerTarParser;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImagePkgMgrDatabase;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.ComponentExtractorFactory;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImageInspector {
    private static final String NO_PKG_MGR_FOUND = "noPkgMgr";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DockerTarParser tarParser;
    private final ComponentExtractorFactory componentExtractorFactory;
    private final Gson gson = new Gson();

    public ImageInspector(final DockerTarParser tarParser, final ComponentExtractorFactory componentExtractorFactory) {
        this.tarParser = tarParser;
        this.componentExtractorFactory = componentExtractorFactory;
    }

    public File getTarExtractionDirectory(final File workingDirectory) {
        return tarParser.getTarExtractionDirectory(workingDirectory);
    }

    public List<File> extractLayerTars(final File workingDir, final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(workingDir, dockerTar);
    }

    public ImageInfoParsed extractDockerLayers(final Gson gson, final OperatingSystemEnum currentOs, final ImageComponentHierarchy imageComponentHierarchy, final File containerFileSystemRootDir, final List<File> layerTars, final ManifestLayerMapping layerMapping) throws IOException,
                                                                                                                                                                                                                                                       WrongInspectorOsException {
        return tarParser.extractDockerLayers(gson, componentExtractorFactory, currentOs, imageComponentHierarchy, containerFileSystemRootDir, layerTars, layerMapping);
    }

    public ManifestLayerMapping getLayerMapping(final GsonBuilder gsonBuilder, final File workingDir, final String tarFileName, final String dockerImageName, final String dockerTagName) throws IntegrationException {
        return tarParser.getLayerMapping(gsonBuilder, workingDir, tarFileName, dockerImageName, dockerTagName);
    }

    public ImageComponentHierarchy createInitialImageComponentHierarchy(final File workingDirectory, final String tarFileName, final ManifestLayerMapping manifestLayerMapping) throws IntegrationException {
        return tarParser.createInitialImageComponentHierarchy(workingDirectory, tarFileName, manifestLayerMapping);
    }

    public ImageInfoDerived generateBdioFromGivenComponents(final BdioGenerator bdioGenerator, ImageInfoParsed imageInfoParsed, final ImageComponentHierarchy imageComponentHierarchy, final ManifestLayerMapping mapping, final String projectName,
        final String versionName,
        final String codeLocationPrefix,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents) {
        final ImageInfoDerived imageInfoDerived = deriveImageInfo(mapping, projectName, versionName, codeLocationPrefix, imageInfoParsed);
        imageInfoDerived.setImageComponentHierarchy(imageComponentHierarchy);
        final SimpleBdioDocument bdioDocument = bdioGenerator.generateBdioDocumentFromImageComponentHierarchy(imageInfoDerived.getCodeLocationName(),
            imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getImageInfoParsed().getLinuxDistroName(), imageComponentHierarchy, organizeComponentsByLayer, includeRemovedComponents);
        imageInfoDerived.setBdioDocument(bdioDocument);
        return imageInfoDerived;
    }

    private ImageInfoDerived deriveImageInfo(final ManifestLayerMapping mapping, final String projectName, final String versionName,
            final String codeLocationPrefix, final ImageInfoParsed imageInfoParsed) {
        logger.debug(String.format("generateBdioFromGivenComponents(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfoDerived imageInfoDerived = new ImageInfoDerived(imageInfoParsed);
        final ImagePkgMgrDatabase imagePkgMgr = imageInfoDerived.getImageInfoParsed().getPkgMgr();
        imageInfoDerived.setManifestLayerMapping(mapping);
        if (imagePkgMgr != null && imagePkgMgr.getPackageManager() != PackageManagerEnum.NULL) {
            imageInfoDerived.setPkgMgrFilePath(determinePkgMgrFilePath(imageInfoDerived.getImageInfoParsed(), imageInfoDerived.getImageInfoParsed().getFileSystemRootDir().getName()));
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    imageInfoDerived.getImageInfoParsed().getPkgMgr().getPackageManager().toString()));
        } else {
            imageInfoDerived.setPkgMgrFilePath(NO_PKG_MGR_FOUND);
            imageInfoDerived.setCodeLocationName(Names.getCodeLocationName(codeLocationPrefix, imageInfoDerived.getManifestLayerMapping().getImageName(), imageInfoDerived.getManifestLayerMapping().getTagName(),
                    NO_PKG_MGR_FOUND));
        }
        imageInfoDerived.setFinalProjectName(deriveBlackDuckProject(imageInfoDerived.getManifestLayerMapping().getImageName(), projectName));
        imageInfoDerived.setFinalProjectVersionName(deriveBlackDuckProjectVersion(imageInfoDerived.getManifestLayerMapping(), versionName));
        logger.info(String.format("Black Duck project: %s, version: %s; Code location : %s", imageInfoDerived.getFinalProjectName(), imageInfoDerived.getFinalProjectVersionName(), imageInfoDerived.getCodeLocationName()));
        return imageInfoDerived;
    }

    private String determinePkgMgrFilePath(final ImageInfoParsed imageInfo, final String imageDirectoryName) {
        String pkgMgrFilePath = imageInfo.getPkgMgr().getExtractedPackageManagerDirectory().getAbsolutePath();
        pkgMgrFilePath = pkgMgrFilePath.substring(pkgMgrFilePath.indexOf(imageDirectoryName) + imageDirectoryName.length() + 1);
        return pkgMgrFilePath;
    }


    private String deriveBlackDuckProject(final String imageName, final String projectName) {
        String blackDuckProjectName;
        if (StringUtils.isBlank(projectName)) {
            blackDuckProjectName = Names.getblackDuckProjectNameFromImageName(imageName);
        } else {
            logger.debug("Using project from config property");
            blackDuckProjectName = projectName;
        }
        return blackDuckProjectName;
    }

    private String deriveBlackDuckProjectVersion(final ManifestLayerMapping mapping, final String versionName) {
        String blackDuckVersionName;
        if (StringUtils.isBlank(versionName)) {
            blackDuckVersionName = mapping.getTagName();
        } else {
            logger.debug("Using project version from config property");
            blackDuckVersionName = versionName;
        }
        return blackDuckVersionName;
    }
}
