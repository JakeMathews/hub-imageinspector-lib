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
package com.synopsys.integration.blackduck.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageComponentHierarchy;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.LayerDetails;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.blackduck.imageinspector.linux.Os;
import com.synopsys.integration.blackduck.imageinspector.linux.extractor.BdioGenerator;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";

    private ImageInspector imageInspector;
    private Os os;

    public ImageInspectorApi(ImageInspector imageInspector, Os os) {
        this.imageInspector = imageInspector;
        this.os = os;
    }

    /**
     * Get a BDIO object representing the packages found in the docker image in the given tarfile. If the tarfile contains
     * more than one image, givenImageRepo and givenImageTag are used to select an image. If containerFileSystemOutputPath
     * is provided, this method will also write the container filesystem (reconstructed as part of the processing
     * required to read the image's packages) to that file as a .tar.gz file.
     *
     * @param dockerTarfilePath Required. The path to the docker image tarfile (produced using the "docker save" command).
     * @param blackDuckProjectName Optional. The Black Duck project name.
     * @param blackDuckProjectVersion Optional. The Black Duck project version.
     * @param codeLocationPrefix Optional. A String to be pre-pended to the generated code location name.
     * @param givenImageRepo Optional. The image repo name. Required only if the given tarfile contains multiple images.
     * @param givenImageTag Optional. The image repo tag.  Required only if the given tarfile contains multiple images.
     * @param cleanupWorkingDir If false, files will be left behind that might be useful for troubleshooting. Should usually be set to true.
     * @param containerFileSystemOutputPath Optional. The path to which the re-constructed container filesystem will be written as a .tar.gz file.
     * @param currentLinuxDistro Optional. The name of the Linux distro (from the ID field of /etc/os-release or /etc/lsb-release) of the machine on which this code is running.
     * @return The generated BDIO object representing the componets (packages) read from the images's package manager database.
     * @throws IntegrationException
     */
    public SimpleBdioDocument getBdio(final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion,
            final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir,
            final String containerFileSystemOutputPath,
            final String currentLinuxDistro)
            throws IntegrationException {
        logger.info("getBdio()::");
        os.logMemory();
        final BdioGenerator bdioGenerator = new BdioGenerator(new SimpleBdioFactory());
        return getBdioDocument(bdioGenerator, dockerTarfilePath, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir, containerFileSystemOutputPath,
                currentLinuxDistro);
    }

    private SimpleBdioDocument getBdioDocument(final BdioGenerator bdioGenerator, final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo,
            final String givenImageTag,
            final boolean cleanupWorkingDir,
            final String containerFileSystemOutputPath, final String currentLinuxDistro)
            throws IntegrationException {
        final ImageInfoDerived imageInfoDerived = inspect(bdioGenerator, dockerTarfilePath, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, cleanupWorkingDir,
                containerFileSystemOutputPath,
                currentLinuxDistro);
        return imageInfoDerived.getBdioDocument();
    }

    private ImageInfoDerived inspect(final BdioGenerator bdioGenerator, final String dockerTarfilePath, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo, final String givenImageTag,
            final boolean cleanupWorkingDir, final String containerFileSystemOutputPath,
            final String currentLinuxDistro)
            throws IntegrationException {
        final File dockerTarfile = new File(dockerTarfilePath);
        File tempDir;
        try {
            tempDir = createTempDirectory();
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error creating temp dir: %s", e.getMessage()), e);
        }
        ImageInfoDerived imageInfoDerived = null;
        try {
            imageInfoDerived = inspectUsingGivenWorkingDir(bdioGenerator, dockerTarfile, blackDuckProjectName, blackDuckProjectVersion, codeLocationPrefix, givenImageRepo, givenImageTag, containerFileSystemOutputPath, currentLinuxDistro,
                    tempDir, cleanupWorkingDir);
        } catch (IOException | InterruptedException | CompressorException e) {
            throw new IntegrationException(String.format("Error inspecting image: %s", e.getMessage()), e);
        } finally {
            if (cleanupWorkingDir) {
                logger.info(String.format("Deleting working dir %s", tempDir.getAbsolutePath()));
                FileOperations.deleteDirPersistently(tempDir);
            }
        }
        return imageInfoDerived;
    }

    private ImageInfoDerived inspectUsingGivenWorkingDir(final BdioGenerator bdioGenerator, final File dockerTarfile, final String blackDuckProjectName, final String blackDuckProjectVersion, final String codeLocationPrefix, final String givenImageRepo,
            final String givenImageTag,
            final String containerFileSystemOutputPath,
            final String currentLinuxDistro, final File tempDir, final boolean cleanupWorkingDir)
            throws IOException, IntegrationException, WrongInspectorOsException, InterruptedException, CompressorException {
        final File workingDir = new File(tempDir, "working");
        logger.debug(String.format("imageInspector: %s; workingDir: %s", imageInspector, workingDir.getAbsolutePath()));
        final List<File> layerTars = imageInspector.extractLayerTars(workingDir, dockerTarfile);
        final ManifestLayerMapping imageMetadata = imageInspector.getLayerMapping(workingDir, dockerTarfile.getName(), givenImageRepo, givenImageTag);
        final ImageComponentHierarchy imageComponentHierarchy = imageInspector.createInitialImageComponentHierarchy(workingDir, dockerTarfile.getName(), imageMetadata);
        final String imageRepo = imageMetadata.getImageName();
        final String imageTag = imageMetadata.getTagName();
        final File tarExtractionDirectory = imageInspector.getTarExtractionDirectory(workingDir);
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        final File targetImageFileSystemRootDir = new File(targetImageFileSystemParentDir, Names.getTargetImageFileSystemRootDirName(imageRepo, imageTag));
        final OperatingSystemEnum currentOs = os.deriveOs(currentLinuxDistro);
        imageInspector.extractDockerLayers(currentOs, imageComponentHierarchy, targetImageFileSystemRootDir, layerTars, imageMetadata);
        // TODO TEMP
        logger.info(String.format("*** layer dump:"));
        for (LayerDetails layer : imageComponentHierarchy.getLayers()) {
            logger.info(String.format("*** Layer ID %s has %d components", layer.getLayerDotTarDirname(), layer.getComponents().size()));
        }
        if (imageComponentHierarchy.getFinalComponents() == null) {
            logger.info(String.format("*** Final image components list NOT SET"));
        } else {
            logger.info(String.format("*** Final image components list has %d components", imageComponentHierarchy.getFinalComponents().size()));
        }
        logger.info(String.format("*** ==========="));
        // TODO of the remaining code in this method: Some might no longer be necessary?
        // I think we've determined the layer mapping, OS, pkg mgr, everything in ImageInfoDerived?
        // Why is ImageInfoDerived being created later?
        cleanUpLayerTars(cleanupWorkingDir, layerTars);
        OperatingSystemEnum inspectorOs = null;
        ImageInfoDerived imageInfoDerived;
        try {
            final ImageInfoParsed imageInfoParsed = imageInspector.parseImageInfo(targetImageFileSystemRootDir);
            inspectorOs = imageInfoParsed.getPkgMgr().getPackageManager().getInspectorOperatingSystem();
            if (!inspectorOs.equals(currentOs)) {
                final ImageInspectorOsEnum neededInspectorOs = ImageInspectorOsEnum.getImageInspectorOsEnum(inspectorOs);
                final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
                throw new WrongInspectorOsException(neededInspectorOs, msg);
            }
            imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(bdioGenerator, imageInfoParsed, imageRepo, imageTag, imageMetadata, blackDuckProjectName, blackDuckProjectVersion, targetImageFileSystemRootDir,
                    codeLocationPrefix);
        } catch (final PkgMgrDataNotFoundException e) {
            imageInfoDerived = imageInspector.generateEmptyBdio(bdioGenerator, imageRepo, imageTag, imageMetadata, blackDuckProjectName, blackDuckProjectVersion, targetImageFileSystemRootDir,
                    codeLocationPrefix);
        }
        createContainerFileSystemTarIfRequested(targetImageFileSystemRootDir, containerFileSystemOutputPath);
        return imageInfoDerived;
    }

    private void cleanUpLayerTars(final boolean cleanupWorkingDir, final List<File> layerTars) {
        if (cleanupWorkingDir) {
            for (final File layerTar : layerTars) {
                logger.trace(String.format("Deleting %s", layerTar.getAbsolutePath()));
                FileUtils.deleteQuietly(layerTar);
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir, final String containerFileSystemOutputPath) throws IOException, CompressorException {
        if (StringUtils.isNotBlank(containerFileSystemOutputPath)) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(containerFileSystemOutputPath);
            final File containerFileSystemTarFile = new File(containerFileSystemOutputPath);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final LinuxFileSystem containerFileSys = new LinuxFileSystem(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private File createTempDirectory() throws IOException {
        final String suffix = String.format("_%s_%s", Thread.currentThread().getName(), Long.toString(new Date().getTime()));
        final File temp = File.createTempFile("ImageInspectorApi_", suffix);
        logger.info(String.format("Creating working dir %s", temp.getAbsolutePath()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        FileOperations.logFreeDiskSpace(temp);
        return temp;
    }

}
