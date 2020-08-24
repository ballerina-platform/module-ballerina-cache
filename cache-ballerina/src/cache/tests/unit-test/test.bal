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

import ballerina/test;
import ballerina/runtime;

@test:Config {}
function testCreateCache() {
    LruEvictionPolicy lruEvictionPolicy = new;
    CacheConfig config = {
        capacity: 10,
        evictionPolicy: lruEvictionPolicy,
        evictionFactor: 0.2,
        defaultMaxAgeInSeconds: 3600,
        cleanupIntervalInSeconds: 5
    };
    Cache|error cache = trap new(config);
    if (cache is Cache) {
       test:assertEquals(cache.size(), 0);
    } else {
       test:assertFail(cache.toString());
    }

}

@test:Config {}
function testPutNewEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    checkpanic cache.put("Hello", "Ballerina");
    test:assertEquals(cache.size(), 1);
}

@test:Config {}
function testPutExistingEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    Cache cache = new(config);
    checkpanic cache.put(key, "Random value");
    checkpanic cache.put(key, "Ballerina");
    test:assertEquals(cache.size(), 1);
    test:assertEquals(cache.get(key).toString(), "Ballerina");
}

@test:Config {}
function testPutWithMaxAge() {
    int maxAge = 5;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    checkpanic cache.put("Hello", "Ballerina", maxAge);
    runtime:sleep(maxAge * 1000 * 2 + 1000);
    test:assertEquals(cache.size(), 1);
}

@test:Config {}
function testGetExistingEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    string value = "Ballerina";
    Cache cache = new(config);
    checkpanic cache.put(key, value);
    any|CacheError expected = cache.get(key);
    test:assertEquals(expected.toString(), value);
}

@test:Config {}
function testGetNonExistingEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    test:assertEquals(cache.get("Hello").toString(), "error Cache entry from the given key: Hello, is not available.");

}

@test:Config {}
function testGetExpiredEntry() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string key = "Hello";
    string value = "Ballerina";
    Cache cache = new(config);
    int maxAgeInSeconds = 1;
    checkpanic cache.put(key, value, maxAgeInSeconds);
    runtime:sleep(maxAgeInSeconds * 1000 * 2 + 1000);
    test:assertEquals(cache.get(key).toString(), "");
}

@test:Config {}
function testRemove() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key = "Hello";
    string value = "Ballerina";
    checkpanic cache.put(key, value);
    checkpanic cache.invalidate(key);
    test:assertEquals(cache.size(), 0);
}

@test:Config {}
function testRemoveAll() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key1 = "Hello";
    string value1 = "Ballerina";
    checkpanic cache.put(key1, value1);
    string key2 = "Ballerina";
    string value2 = "Language";
    checkpanic cache.put(key2, value2);
    checkpanic cache.invalidateAll();
    test:assertEquals(cache.size(), 0);
}

@test:Config {}
function testHasKey() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key = "Hello";
    string value = "Ballerina";
    checkpanic cache.put(key, value);
    test:assertTrue(cache.hasKey(key));
}

@test:Config {}
function testKeys() {
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
    checkpanic cache.put(key1, value1);
    checkpanic cache.put(key2, value2);
    test:assertEquals(cache.keys(), keys);
}

@test:Config {}
function testCapacity() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    test:assertEquals(cache.capacity(), 10);
}

@test:Config {}
function testSize() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    Cache cache = new(config);
    string key1 = "Hello";
    string value1 = "Ballerina";
    string key2 = "Ballerina";
    string value2 = "Language";
    checkpanic cache.put(key1, value1);
    checkpanic cache.put(key2, value2);
    test:assertEquals(cache.size(), 2);
}

@test:Config {}
function testCacheEvictionWithCapacity1() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string[] keys = ["C", "D", "E", "F", "G", "H", "I", "J", "K"];
    Cache cache = new(config);
    checkpanic cache.put("A", "1");
    checkpanic cache.put("B", "2");
    checkpanic cache.put("C", "3");
    checkpanic cache.put("D", "4");
    checkpanic cache.put("E", "5");
    checkpanic cache.put("F", "6");
    checkpanic cache.put("G", "7");
    checkpanic cache.put("H", "8");
    checkpanic cache.put("I", "9");
    checkpanic cache.put("J", "10");
    checkpanic cache.put("K", "11");
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {}
function testCacheEvictionWithCapacity2() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2
    };
    string[] keys = ["A", "D", "E", "F", "G", "H", "I", "J", "K"];
    Cache cache = new(config);
    checkpanic cache.put("A", "1");
    checkpanic cache.put("B", "2");
    checkpanic cache.put("C", "3");
    checkpanic cache.put("D", "4");
    checkpanic cache.put("E", "5");
    checkpanic cache.put("F", "6");
    checkpanic cache.put("G", "7");
    checkpanic cache.put("H", "8");
    checkpanic cache.put("I", "9");
    checkpanic cache.put("J", "10");
    any|Error x = cache.get("A");
    checkpanic cache.put("K", "11");
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {}
function testCacheEvictionWithTimer1() {
    int cleanupIntervalInSeconds = 2;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAgeInSeconds: 1,
        cleanupIntervalInSeconds: cleanupIntervalInSeconds
    };
    Cache cache = new(config);
    checkpanic cache.put("A", "1");
    checkpanic cache.put("B", "2");
    checkpanic cache.put("C", "3");
    string[] keys = [];
    runtime:sleep(cleanupIntervalInSeconds * 1000 * 2 + 1000);
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {}
function testCacheEvictionWithTimer2() {
    int cleanupIntervalInSeconds = 2;
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAgeInSeconds: 1,
        cleanupIntervalInSeconds: cleanupIntervalInSeconds
    };
    Cache cache = new(config);
    checkpanic cache.put("A", "1");
    checkpanic cache.put("B", "2", 3600);
    checkpanic cache.put("C", "3");
    string[] keys = ["B"];
    runtime:sleep(cleanupIntervalInSeconds * 1000 * 2 + 1000);
    test:assertEquals(cache.size(), keys.length());
    test:assertEquals(cache.keys(), keys);
}

@test:Config {}
function testCreateCacheWithZeroCapacity() {
    CacheConfig config = {
        capacity: 0,
        evictionFactor: 0.2
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(), "error Capacity must be greater than 0.");
}

@test:Config {}
function testCreateCacheWithNegativeCapacity() {
    CacheConfig config = {
        capacity: -1,
        evictionFactor: 0.2
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(), "error Capacity must be greater than 0.");
}

@test:Config {}
function testCreateCacheWithZeroEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(),
    "error Cache eviction factor must be between 0.0 (exclusive) and 1.0 (inclusive).");
}

@test:Config {}
function testCreateCacheWithNegativeEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: -1
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(),
    "error Cache eviction factor must be between 0.0 (exclusive) and 1.0 (inclusive).");
}

@test:Config {}
function testCreateCacheWithInvalidEvictionFactor() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 1.1
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(),
    "error Cache eviction factor must be between 0.0 (exclusive) and 1.0 (inclusive).");
}

@test:Config {}
function testCreateCacheWithZeroDefaultMaxAge() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAgeInSeconds: 0
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(),
    "error Default max age should be greater than 0 or -1 for indicate forever valid.");
}

@test:Config {}
function testCreateCacheWithNegativeDefaultMaxAge() {
    CacheConfig config = {
        capacity: 10,
        evictionFactor: 0.2,
        defaultMaxAgeInSeconds: -10
    };
    Cache|error cache = trap new(config);
    test:assertTrue(cache is error);
    test:assertEquals(cache.toString(),
    "error Default max age should be greater than 0 or -1 for indicate forever valid.");
}
