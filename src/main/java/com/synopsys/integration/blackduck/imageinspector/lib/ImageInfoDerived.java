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

import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.ImageInfoParsed;
import com.synopsys.integration.blackduck.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;

public class ImageInfoDerived {
    private final ImageInfoParsed imageInfoParsed;
    private ManifestLayerMapping manifestLayerMapping = null;
    private String pkgMgrFilePath = null;

    private String codeLocationName = null;
    private String finalProjectName = null;
    private String finalProjectVersionName = null;

    private ImageComponentHierarchy imageComponentHierarchy = null;
    private SimpleBdioDocument bdioDocument = null;

    public ImageInfoDerived(final ImageInfoParsed imageInfoParsed) {
        this.imageInfoParsed = imageInfoParsed;
    }

    public ManifestLayerMapping getManifestLayerMapping() {
        return manifestLayerMapping;
    }

    public void setManifestLayerMapping(final ManifestLayerMapping manifestLayerMapping) {
        this.manifestLayerMapping = manifestLayerMapping;
    }

    public String getPkgMgrFilePath() {
        return pkgMgrFilePath;
    }

    public void setPkgMgrFilePath(final String pkgMgrFilePath) {
        this.pkgMgrFilePath = pkgMgrFilePath;
    }

    public ImageInfoParsed getImageInfoParsed() {
        return imageInfoParsed;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public void setCodeLocationName(final String codeLocationName) {
        this.codeLocationName = codeLocationName;
    }

    public String getFinalProjectName() {
        return finalProjectName;
    }

    public void setFinalProjectName(final String finalProjectName) {
        this.finalProjectName = finalProjectName;
    }

    public String getFinalProjectVersionName() {
        return finalProjectVersionName;
    }

    public void setFinalProjectVersionName(final String finalProjectVersionName) {
        this.finalProjectVersionName = finalProjectVersionName;
    }

    public SimpleBdioDocument getBdioDocument() {
        return bdioDocument;
    }

    public void setBdioDocument(final SimpleBdioDocument bdioDocument) {
        this.bdioDocument = bdioDocument;
    }

    public void setImageComponentHierarchy(final ImageComponentHierarchy imageComponentHierarchy) {
        this.imageComponentHierarchy = imageComponentHierarchy;
    }

    public ImageComponentHierarchy getImageComponentHierarchy() {
        return imageComponentHierarchy;
    }
}
