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

/**
 * Constants used in compiler plugin.
 */
public class Constants {
    public static final String BALLERINA = "ballerina";
    public static final String CACHE = "cache";
    public static final String CACHE_CONFIG = "CacheConfig";

    public static final String CAPACITY = "capacity";
    public static final String CLEAN_UP_INTERVAL = "cleanupInterval";
    public static final String EVICTION_FACTOR = "evictionFactor";
    public static final String EVICTION_POLICY = "evictionPolicy";
    public static final String DEFAULT_MAX_AGE = "defaultMaxAge";
    public static final String POLICY_VALUE = "cache:LRU";
    public static final String UNNECESSARY_CHARS_REGEX = "\"|\\n";
}
