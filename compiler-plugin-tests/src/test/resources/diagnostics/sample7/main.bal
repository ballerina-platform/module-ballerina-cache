// Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org).
//
// WSO2 Inc. licenses this file to you under the Apache License,
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

import ballerina/cache;

configurable int capacity = 300;
const decimal CACHE_CLEANUP_INTERVAL = 900.0;

public function main() returns error? {
    cache:Cache cache = new(capacity = capacity , evictionFactor = 0.2, defaultMaxAge = 86400, cleanupInterval = CACHE_CLEANUP_INTERVAL);
}
