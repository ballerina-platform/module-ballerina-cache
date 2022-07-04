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

import ballerina/jballerina.java;
import ballerina/task;
import ballerina/time;

# Represents configurations for the `cache:Cache` object.
#
# + capacity - Maximum number of entries allowed in the cache
# + evictionFactor - The factor by which the entries will be evicted once the cache is full
# + evictionPolicy - The policy which is used to evict entries once the cache is full
# + defaultMaxAge - The max-age in seconds which all the cache entries are valid. '-1' means, the entries are
#                   valid forever. This will be overwritten by the `maxAge` property set when inserting item into
#                   the cache
# + cleanupInterval - Interval (in seconds) of the timer task, which will clean up the cache
public type CacheConfig record {|
    int capacity = 100;
    float evictionFactor = 0.25;
    EvictionPolicy evictionPolicy = LRU;
    decimal defaultMaxAge = -1;
    decimal cleanupInterval?;
|};

# Possible types of eviction policy that can be passed into the `EvictionPolicy`.
public enum EvictionPolicy {
    LRU
}

type CacheEntry record {|
    any data;
    decimal expTime;  // exp time since epoch. calculated based on the `maxAge` parameter when inserting to map
|};

// Cleanup service which cleans the cache entries periodically.
boolean cleanupInProgress = false;

class Cleanup {

    *task:Job;
    private Cache cache;

    public function execute() {
        // This check will skip the processes triggered while the clean up in progress.
        if !cleanupInProgress {
            cleanupInProgress = true;
            time:Utc currentUtc = time:utcNow();
            externCleanUp(self.cache, <decimal>currentUtc[0] + currentUtc[1]);
            cleanupInProgress = false;
        }
    }

    public isolated function init(Cache cache) {
        self.cache = cache;
    }
}

# The `cache:Cache` object, which is used for all the cache-related operations. It is not recommended to insert `()`
# as the value of the cache since it doesn't make any sense to cache a nil.
public isolated class Cache {

    *AbstractCache;

    private final int maxCapacity;
    private final EvictionPolicy evictionPolicy;
    private final float evictionFactor;
    private final decimal defaultMaxAge;

    # Initializes new `cache:Cache` instance.
    # ```ballerina
    # cache:Cache cache = new(capacity = 10, evictionFactor = 0.2);
    # ```
    #
    # + cacheConfig - Configurations for the `cache:Cache` object
    public isolated function init(*CacheConfig cacheConfig) {
        self.maxCapacity = cacheConfig.capacity;
        self.evictionPolicy = cacheConfig.evictionPolicy;
        self.evictionFactor = cacheConfig.evictionFactor;
        self.defaultMaxAge =  cacheConfig.defaultMaxAge;

        externInit(self);
        decimal? interval = cacheConfig?.cleanupInterval;
        if interval is decimal {
            time:Utc currentUtc = time:utcNow();
            time:Utc newTime = time:utcAddSeconds(currentUtc, interval);
            time:Civil time = time:utcToCivil(newTime);
            var result = task:scheduleJobRecurByFrequency(new Cleanup(self), interval, startTime = time);
            if (result is task:Error) {
                panic prepareError(string `Failed to schedule the cleanup task: ${result.message()}`);
            }
        }
    }

    # Adds the given key value pair to the cache. If the cache previously contained a value associated with the
    # provided key, the old value will be replaced by the newly-provided value.
    # ```ballerina
    # check cache.put("Hello", "Ballerina");
    # ```
    #
    # + key - Key of the value to be cached
    # + value - Value to be cached. Value should not be `()`
    # + maxAge - The time in seconds for which the cache entry is valid. If the value is '-1', the entry is
    #                     valid forever.
    # + return - `()` if successfully added to the cache or a `cache:Error` if a `()` value is inserted to the cache.
    public isolated function put(string key, any value, decimal maxAge = -1) returns Error? {
        if value is () {
            return prepareError("Unsupported cache value '()' for the key: " + key + ".");
        }

        time:Utc currentUtc = time:utcNow();
        // Calculate the `expTime` of the cache entry based on the `maxAgeInSeconds` property and
        // `defaultMaxAge` property.
        decimal calculatedExpTime = -1;
        if maxAge != -1d && maxAge > 0d {
            time:Utc newTime = time:utcAddSeconds(currentUtc, <decimal> maxAge);
            calculatedExpTime = <decimal>newTime[0] + newTime[1];
        } else {
            if self.defaultMaxAge != -1d {
                time:Utc newTime = time:utcAddSeconds(currentUtc, <decimal> self.defaultMaxAge);
                calculatedExpTime = <decimal>newTime[0] + newTime[1];
            }
        }

        CacheEntry entry = {
            data: value,
            expTime: calculatedExpTime
        };
        return externPut(self, key, entry);
    }

    # Returns the cached value associated with the provided key.
    # ```ballerina
    # any value = check cache.get(key);
    # ```
    #
    # + key - Key of the cached value, which should be retrieved
    # + return - The cached value associated with the provided key or a `cache:Error` if the provided cache key is not
    #            exisiting in the cache or any error occurred while retrieving the value from the cache.
    public isolated function get(string key) returns any|Error {
        time:Utc currentUtc = time:utcNow();
        any? entry = externGet(self, key, <decimal>currentUtc[0] + currentUtc[1]);
        if entry is CacheEntry {
            return entry.data;
        } else {
            return prepareError("Cache entry from the given key: " + key + ", is not available.");
        }
    }

    # Discards a cached value from the cache.
    # ```ballerina
    # check cache.invalidate(key);
    # ```
    #
    # + key - Key of the cache value, which needs to be discarded from the cache
    # + return - `()` if successfully discarded the value or a `cache:Error` if the provided cache key is not present
    #            in the cache
    public isolated function invalidate(string key) returns Error? {
        if !self.hasKey(key) {
            return prepareError("Cache entry from the given key: " + key + ", is not available.");
        }
        externRemove(self, key);
    }

    # Discards all the cached values from the cache.
    # ```ballerina
    # check cache.invalidateAll();
    # ```
    #
    # + return - `()` if successfully discarded all the values from the cache or a `cache:Error` if any error
    #            occurred while discarding all the values from the cache.
    public isolated function invalidateAll() returns Error? {
        externRemoveAll(self);
    }

    # Checks whether the given key has an associated cached value.
    # ```ballerina
    # boolean result = cache.hasKey(key);
    # ```
    #
    # + key - The key to be checked in the cache
    # + return - `true` if a cached value is available for the provided key or `false` if there is no cached value
    #            associated for the given key
    public isolated function hasKey(string key) returns boolean {
        return externHasKey(self, key);
    }

    # Returns a list of all the keys from the cache.
    # ```ballerina
    # string[] keys = cache.keys();
    # ```
    #
    # + return - Array of all the keys from the cache
    public isolated function keys() returns string[] {
        return externKeys(self);
    }

    # Returns the size of the cache.
    # ```ballerina
    # int result = cache.size();
    # ```
    #
    # + return - The size of the cache
    public isolated function size() returns int {
        return externSize(self);
    }

    # Returns the capacity of the cache.
    # ```ballerina
    # int result = cache.capacity();
    # ```
    #
    # + return - The capacity of the cache
    public isolated function capacity() returns int {
        return self.maxCapacity;
    }
}

isolated function externInit(Cache cache) = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externRemoveAll(Cache cache) = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externHasKey(Cache cache, string key) returns boolean = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externKeys(Cache cache) returns string[] = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externSize(Cache cache) returns int = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externPut(Cache cache, string key, any value) = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externGet(Cache cache, string key, decimal currentTime) returns CacheEntry? = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externRemove(Cache cache, string key) = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externCleanUp(Cache cache, decimal currentTime) = @java:Method {
    'class: "io.ballerina.stdlib.cache.nativeimpl.Cache"
} external;
