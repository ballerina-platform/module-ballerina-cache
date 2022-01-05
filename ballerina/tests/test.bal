// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/lang.runtime as runtime;
import ballerina/lang.'string as strings;
import ballerina/test;

@test:Config {
    groups: ["create"]
}
isolated function testCreateCache() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAge: 3600,
        cleanupInterval: 5,
        evictionPolicy: LRU
    };
    Cache|error cache = trap new(config);
    if cache is Cache {
       test:assertEquals(cache.size(), 0);
    } else {
       test:assertFail(cache.toString());
    }
}

@test:Config {
    groups: ["create", "put", "size"]
}
isolated function testPutNewEntry() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    check cache.put("Hello", "Ballerina");
    test:assertEquals(cache.size(), 1);
}

@test:Config {
    groups: ["create", "put", "size", "entry"]
}
isolated function testPutExistingEntry() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    Cache cache = new(config);
    check cache.put(key, "Random value");
    check cache.put(key, "Ballerina");
    test:assertEquals(cache.size(), 1);
    any results = check cache.get(key);
    test:assertEquals(results.toString(), "Ballerina");
}

@test:Config {
    groups: ["create", "put", "size", "age"]
}
isolated function testPutWithMaxAge() returns error? {
    decimal maxAge = 5;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    check cache.put("Hello", "Ballerina", maxAge);
    decimal sleepTime = maxAge * 2 + 1;
    runtime:sleep(sleepTime);
    test:assertEquals(cache.size(), 1);
}

@test:Config {
    groups: ["create", "get"]
}
isolated function testGetExistingEntry() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    string value = "Ballerina";
    Cache cache = new(config);
    _ = check cache.put(key, value);
    any expected = check cache.get(key);
    test:assertEquals(expected.toString(), value);
}

@test:Config {
    groups: ["create", "get"]
}
isolated function testGetNonExistingEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    any|error expected = cache.get("Hello");
    if expected is error {
        test:assertEquals(expected.toString(), "error Error (\"Cache entry from the given key: " +
                              "Hello, is not available.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["create", "put", "size", "expired", "get"]
}
isolated function testGetExpiredEntry() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    string value = "Ballerina";
    Cache cache = new(config);
    decimal maxAgeInSeconds = 1;
    check cache.put(key, value, maxAgeInSeconds);
    decimal sleepTime = maxAgeInSeconds * 2 + 1;
    runtime:sleep(sleepTime);
    any expected = check cache.get(key);
    test:assertEquals(expected.toString(), "");
}

@test:Config {
    groups: ["create", "put", "size", "remove", "invalidate"]
}
isolated function testRemove() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key = "Hello";
    string value = "Ballerina";
    check cache.put(key, value);
    check cache.invalidate(key);
    test:assertEquals(cache.size(), 0);
}

@test:Config {
    groups: ["create", "put", "size", "remove", "invalidate"]
}
isolated function testRemoveAll() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key1 = "Hello";
    string value1 = "Ballerina";
    check cache.put(key1, value1);
    string key2 = "Ballerina";
    string value2 = "Language";
    check cache.put(key2, value2);
    check cache.invalidateAll();
    test:assertEquals(cache.size(), 0);
}

@test:Config {
    groups: ["create", "get", "key"]
}
isolated function testHasKey() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key = "Hello";
    string value = "Ballerina";
    check cache.put(key, value);
    test:assertTrue(cache.hasKey(key));
}

@test:Config {
    groups: ["create", "get", "keys"]
}
isolated function testKeys() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key1 = "Hello";
    string value1 = "Ballerina";
    string key2 = "Ballerina";
    string value2 = "Language";
    string[] keys = [key1, key2];
    check cache.put(key1, value1);
    check cache.put(key2, value2);
    test:assertEquals(cache.keys(), keys);
}

@test:Config {
    groups: ["create", "capacity"]
}
isolated function testCapacity() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    test:assertEquals(cache.capacity(), 10);
}

@test:Config {
    groups: ["cache", "resize"]
}
isolated function testSize() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key1 = "Hello";
    string value1 = "Ballerina";
    string key2 = "Ballerina";
    string value2 = "Language";
    check cache.put(key1, value1);
    check cache.put(key2, value2);
    test:assertEquals(cache.size(), 2);
}

@test:Config {
    groups: ["cache", "capacity", "policy"]
}
isolated function testCacheEvictionWithCapacity1() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string[] keys = ["C", "D", "E", "F", "G", "H", "I", "J", "K"];
    Cache cache = new(config);
    check cache.put("A", "1");
    check cache.put("B", "2");
    check cache.put("C", "3");
    check cache.put("D", "4");
    check cache.put("E", "5");
    check cache.put("F", "6");
    check cache.put("G", "7");
    check cache.put("H", "8");
    check cache.put("I", "9");
    check cache.put("J", "10");
    check cache.put("K", "11");
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {
    groups: ["cache", "capacity", "policy"]
}
isolated function testCacheEvictionWithCapacity2() returns error? {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string[] keys = ["A", "D", "E", "F", "G", "H", "I", "J", "K"];
    Cache cache = new(config);
    check cache.put("A", "1");
    check cache.put("B", "2");
    check cache.put("C", "3");
    check cache.put("D", "4");
    check cache.put("E", "5");
    check cache.put("F", "6");
    check cache.put("G", "7");
    check cache.put("H", "8");
    check cache.put("I", "9");
    check cache.put("J", "10");
    _ = check cache.get("A");
    check cache.put("K", "11");
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {
    groups: ["cache", "capacity", "policy"]
}
isolated function testCacheEvictionWithTimer1() returns error? {
    decimal cleanupInterval = 2;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAge: 1,
        cleanupInterval: cleanupInterval
    };
    Cache cache = new(config);
    check cache.put("A", "1");
    check cache.put("B", "2");
    check cache.put("C", "3");
    string[] keys = [];
    decimal sleepTime = cleanupInterval * 2 + 2;
    runtime:sleep(sleepTime);
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {
    groups: ["cache", "capacity", "policy"]
}
isolated function testCacheEvictionWithTimer2() returns error? {
    decimal cleanupInterval = 2;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAge: 1,
        cleanupInterval: cleanupInterval
    };
    Cache cache = new(config);
    check cache.put("A", "1");
    check cache.put("B", "2", 3600);
    check cache.put("C", "3");
    string[] keys = ["B"];
    decimal sleepTime = cleanupInterval * 2 + 1;
    runtime:sleep(sleepTime);
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {
    groups: ["cache", "capacity", "policy"]
}
isolated function testCreateCacheWithZeroCapacity() {
    CacheConfig config = {
        capacity: 0,
        evictionFactor: 0.2
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Capacity must be greater than 0.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "policy", "negative"]
}
isolated function testCreateCacheWithNegativeCapacity() {
    CacheConfig config = {
        capacity: -1,
        evictionFactor: 0.2
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Capacity must be greater than 0.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "policy", "negative"]
}
isolated function testCreateCacheWithZeroEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Cache eviction factor must be between 0.0 (exclusive)" +
                              " and 1.0 (inclusive).\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "policy", "negative"]
}
isolated function testCreateCacheWithNegativeEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: -1
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Cache eviction factor must be between 0.0 " +
                              "(exclusive) and 1.0 (inclusive).\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "policy", "negative"]
}
isolated function testCreateCacheWithInvalidEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 1.1
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Cache eviction factor must be between 0.0 " +
                              "(exclusive) and 1.0 (inclusive).\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "policy", "negative"]
}
isolated function testCreateCacheWithZeroDefaultMaxAge() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAge: 0
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Default max age should be greater " +
                              "than 0 or -1 for indicate forever valid.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "capacity", "age", "negative"]
}
isolated function testCreateCacheWithNegativeDefaultMaxAge() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAge: -10
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.toString(), "error Error (\"Default max age should be greater than 0 or -1 " +
                              "for indicate forever valid.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "cleanup", "negative"]
}
isolated function testCreateCacheWithNegativeCleanUpInterval() {
    CacheConfig config = {
        cleanupInterval: -1d
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertEquals(cache.message(), "The cleanup interval must be a positive value.");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "cleanup", "negative"]
}
isolated function testCleanUpTaskStartTime() {
    CacheConfig config = {
        cleanupInterval: 0
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    if (cache is error) {
        test:assertTrue(strings:includes(cache.message(), "Scheduled time should be greater than the current time"),
        cache.message());
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "put", "negative"]
}
isolated function testPutWithNullValue() {
    Cache cache = new();
    error? result = cache.put("A", ());
    test:assertTrue(result is error);
    if (result is error) {
        test:assertEquals(result.toString(), "error Error (\"Unsupported cache value '()' for the key: A.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "invalidate", "negative"]
}
isolated function testInvalidateWithNonExistingValue() {
    Cache cache = new();
    error? result = cache.invalidate("A");
    test:assertTrue(result is error);
    if (result is error) {
        test:assertEquals(result.toString(), "error Error (\"Cache entry from the given key: A, is not available.\")");
    } else {
         test:assertFail("Output mismatched");
    }
}

@test:Config {
    groups: ["cache", "Eviction"]
}
isolated function testEvictionCount() returns error? {
    CacheConfig config = {
        capacity: 1,
        evictionFactor: 0.1
    };
    Cache cache = new(config);
    check cache.put("A", "1");
    check cache.put("B", "2");
    check cache.put("C", "3");
    string[] keys = ["C"];
    test:assertEquals(cache.size(), keys.length(), "Cache size did not match");
    test:assertEquals(cache.keys(), keys, "Cache keys did not match");
}
