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
package com.synopsys.integration.blackduck.imageinspector.linux.executor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;

@Component
public class RpmExecutor extends PkgMgrExecutor {
    private static final String PACKAGE_FORMAT_STRING = "\\{ epoch: \"%{E}\", name: \"%{N}\", version: \"%{V}-%{R}\", arch: \"%{ARCH}\" \\}\\n";

    @Override
    @PostConstruct
    public void init() {
        initValues("rpm --rebuilddb",
                Arrays.asList("rpm", "-qa", "--qf", PACKAGE_FORMAT_STRING));
    }

    @Override
    protected void initPkgMgrDir(final File packageManagerDirectory) throws IOException {
        FileOperations.deleteFilesOnly(packageManagerDirectory);
    }
}
