/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.cache.compiler;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import static io.ballerina.tools.diagnostics.DiagnosticSeverity.ERROR;

/**
 * Enum class to hold cache module diagnostic codes.
 */
public enum DiagnosticsCodes {

    CACHE_101("invalid value: a greater than zero value is expected", "CACHE_101", ERROR),
    CACHE_102("invalid value: a value between 0 (exclusive) and 1 (inclusive) is expected",
            "CACHE_102", ERROR),
    CACHE_103("invalid value: a greater than 0 value or -1(to indicate forever valid) value is expected",
            "CACHE_103", ERROR),
    CACHE_104("invalid value: a greater than zero value is expected", "CACHE_104", ERROR),
    CACHE_105("invalid value: only 'cache:LRU' value is supported", "CACHE_105", ERROR),
    CACHE_106("invalid value: ", "CACHE_106", ERROR);

    private final String error;
    private final String errorCode;
    private final DiagnosticSeverity severity;

    DiagnosticsCodes(String error, String errorCode, DiagnosticSeverity severity) {
        this.error = error;
        this.errorCode = errorCode;
        this.severity = severity;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
