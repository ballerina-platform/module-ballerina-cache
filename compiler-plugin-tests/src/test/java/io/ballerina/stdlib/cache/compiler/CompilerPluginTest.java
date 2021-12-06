/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.cache.compiler;

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests the custom cache compiler plugin.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "diagnostics")
            .toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Paths.get("../", "target", "ballerina-runtime")
            .toAbsolutePath();

    private static ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    @Test
    public void testInvalidConnectionParamConfig() {
        Package currentPackage = loadPackage("sample1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnosticsList = diagnosticResult.diagnostics().stream()
                .filter(r -> r.diagnosticInfo().severity().equals(DiagnosticSeverity.ERROR))
                .collect(Collectors.toList());
        long availableErrors = errorDiagnosticsList.size();

        Assert.assertEquals(availableErrors, 4);

        DiagnosticInfo invalidCapacity = errorDiagnosticsList.get(0).diagnosticInfo();
        Assert.assertEquals(invalidCapacity.code(), DiagnosticsCodes.CACHE_101.getErrorCode());
        Assert.assertEquals(invalidCapacity.messageFormat(),
                "invalid value: expected value is greater than zero");

        DiagnosticInfo invalidEvictionFactor = errorDiagnosticsList.get(1).diagnosticInfo();
        Assert.assertEquals(invalidEvictionFactor.code(), DiagnosticsCodes.CACHE_102.getErrorCode());
        Assert.assertEquals(invalidEvictionFactor.messageFormat(),
                "invalid value: expected value is between 0 (exclusive) and 1 (inclusive)");


        DiagnosticInfo invalidDefaultMaxAge = errorDiagnosticsList.get(2).diagnosticInfo();
        Assert.assertEquals(invalidDefaultMaxAge.code(), DiagnosticsCodes.CACHE_103.getErrorCode());
        Assert.assertEquals(invalidDefaultMaxAge.messageFormat(),
                "invalid value: expected value is greater than 0 or -1 for indicate forever valid");

        DiagnosticInfo invalidCleanupInterval = errorDiagnosticsList.get(3).diagnosticInfo();
        Assert.assertEquals(invalidCleanupInterval.code(), DiagnosticsCodes.CACHE_104.getErrorCode());
        Assert.assertEquals(invalidCleanupInterval.messageFormat(),
                "invalid value: expected value is greater than zero");
    }
}
