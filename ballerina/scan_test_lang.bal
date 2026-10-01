// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org)
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

const API_KEY = "234567890234567890-34567890-dkajf";

string passwordPrefix = "pass";
string passwordSuffix = "word";

function connect(string username) returns string {
    string password = "password";
    return username + password + API_KEY;
}

function derivedSecret() returns string {
    string password = passwordPrefix + passwordSuffix;
    return password;
}

function parseCount(string value, int unused) returns int {
    int count = checkpanic int:fromString(value);
    count = count;
    if count == count {
        return count;
    }
    return 0;
}

isolated function externAbs(int value) returns int = @java:Method {
    'class: "java.lang.Math",
    name: "abs",
    paramTypes: ["long"]
} external;
