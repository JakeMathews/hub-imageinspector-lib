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
package com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.synopsys.integration.blackduck.imageinspector.name.ImageNameResolver;

public class Manifest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File tarExtractionDirectory;
    private final String dockerTarFileName;

    private ManifestLayerMappingFactory manifestLayerMappingFactory;

    public Manifest(final File tarExtractionDirectory, final String dockerTarFileName) {
        this.tarExtractionDirectory = tarExtractionDirectory;
        this.dockerTarFileName = dockerTarFileName;
    }

    public void setManifestLayerMappingFactory(final ManifestLayerMappingFactory manifestLayerMappingFactory) {
        this.manifestLayerMappingFactory = manifestLayerMappingFactory;
    }

    public ManifestLayerMapping getLayerMapping(final String targetImageName, final String targetTagName) throws IntegrationException, IOException {
        logger.debug(String.format("getLayerMappings(): targetImageName: %s; targetTagName: %s", targetImageName, targetTagName));
        final List<ManifestLayerMapping> mappings = new ArrayList<>();
        final List<ImageInfo> images = getManifestContents();
        logger.debug(String.format("getLayerMappings(): images.size(): %d", images.size()));
        validateImageSpecificity(images, targetImageName, targetTagName);
        for (final ImageInfo image : images) {
            logger.debug(String.format("getLayerMappings(): image: %s", image));
            final String foundRepoTag = findRepoTag(images.size(), image, targetImageName, targetTagName);
            if (foundRepoTag == null) {
                continue;
            }
            logger.debug(String.format("foundRepoTag: %s", foundRepoTag));
            final ImageNameResolver resolver = new ImageNameResolver(foundRepoTag);
            logger.debug(String.format("translated repoTag to: repo: %s, tag: %s", resolver.getNewImageRepo().get(), resolver.getNewImageTag().get()));
            return createMapping(image, resolver.getNewImageRepo().get(), resolver.getNewImageTag().get());
        }
        throw new IntegrationException(String.format("Layer mapping for repo:tag %s:%s not found in manifest.json", targetImageName, targetTagName));
    }

    private String findRepoTag(final int numImages, final ImageInfo image, final String targetImageName, final String targetTagName) throws IntegrationException {
        // user didn't specify which image, and there is only one: return it
        if (numImages == 1 && StringUtils.isBlank(targetImageName) && StringUtils.isBlank(targetTagName)) {
            logger.debug(String.format("User did not specify a repo:tag, and there's only one imamge; inspecting that one: %s", getRepoTag(image)));
            return getRepoTag(image);
        }
        final String targetRepoTag = deriveSpecifiedRepoTag(targetImageName, targetTagName);
        logger.debug(String.format("findRepoTag(): specifiedRepoTag: %s", targetRepoTag));
        for (final String repoTag : image.repoTags) {
            logger.trace(String.format("Target repo tag %s; checking %s", targetRepoTag, repoTag));
            if (StringUtils.compare(repoTag, targetRepoTag) == 0) {
                logger.trace(String.format("Found the targetRepoTag %s", targetRepoTag));
                return repoTag;
            }
        }
        return null;
    }

    private String getRepoTag(final ImageInfo image) {
        if (image.repoTags == null || image.repoTags.size() == 0) {
            return "null:null";
        }
        return image.repoTags.get(0);
    }

    private ManifestLayerMapping createMapping(final ImageInfo image, final String imageName, final String tagName) {
        final List<String> layerIds = new ArrayList<>();
        for (final String layer : image.layers) {
            layerIds.add(layer.substring(0, layer.indexOf('/')));
        }
        final ManifestLayerMapping mapping = manifestLayerMappingFactory.createManifestLayerMapping(imageName, tagName, image.config, layerIds);
        logger.debug(String.format("Found layer mapping: Image %s, Tag %s, Layers: %s", mapping.getImageName(), mapping.getTagName(), mapping.getLayers()));
        return mapping;
    }

    private String deriveSpecifiedRepoTag(final String dockerImageName, final String dockerTagName) {
        String specifiedRepoTag = "";
        if (StringUtils.isNotBlank(dockerImageName)) {
            specifiedRepoTag = String.format("%s:%s", dockerImageName, dockerTagName);
        }
        return specifiedRepoTag;
    }

    private void validateImageSpecificity(final List<ImageInfo> images, final String targetImageName, final String targetTagName) throws IntegrationException {
        if (images.size() > 1 && (StringUtils.isBlank(targetImageName) || StringUtils.isBlank(targetTagName))) {
            final String msg = "When the manifest contains multiple images or tags, the target image and tag to inspect must be specified";
            logger.debug(msg);
            throw new IntegrationException(msg);
        }
    }

    private List<ImageInfo> getManifestContents() throws IOException {
        logger.trace("getManifestContents()");
        final List<ImageInfo> images = new ArrayList<>();
        logger.debug("getManifestContents(): extracting manifest file content");
        final String manifestContentString = extractManifestFileContent(dockerTarFileName);
        logger.debug(String.format("getManifestContents(): parsing: %s", manifestContentString));
        final JsonParser parser = new JsonParser();
        final JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray();
        final Gson gson = new Gson();
        for (final JsonElement element : manifestContent) {
            logger.debug(String.format("getManifestContents(): element: %s", element.toString()));
            images.add(gson.fromJson(element, ImageInfo.class));
        }
        return images;
    }

    private String extractManifestFileContent(final String dockerTarName) throws IOException {
        final File dockerTarDirectory = new File(tarExtractionDirectory, dockerTarName);
        final File manifest = new File(dockerTarDirectory, "manifest.json");
        final String manifestFileContents = StringUtils.join(FileUtils.readLines(manifest, StandardCharsets.UTF_8), "\n");
        return manifestFileContents;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
