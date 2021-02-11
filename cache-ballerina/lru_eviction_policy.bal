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

# The `cache:LruEvictionPolicy` object consists of the LRU eviction algorithm related operations based on a linked
# list data structure.
public class LruEvictionPolicy {

    *AbstractEvictionPolicy;

    # Called when a new `cache:LruEvictionPolicy` object is created.
    public isolated function init() {}

    # Updates the cache based on the get operation related to the LRU eviction algorithm.
    #
    # + cache - A `Cache` object
    # + key - A key, which is retrieved
    # + return - A cached value associated with the provided key or an `nill`
    public isolated function get(Cache cache, string key) returns any? {
        return externGet(cache, key);
    }

    # Updates the cache based on the put operation related to the LRU eviction algorithm.
    #
    # + cache - A `Cache` object
    # + key - A key, which is added newly
    # + value - A value
    public isolated function put(Cache cache, string key, any value) {
        externPut(cache, key, value);
    }

    # Updates the cache based on the remove operation related to the LRU eviction algorithm.
    #
    # + cache - A `Cache` object
    # + key - A key, which is deleted
    public isolated function remove(Cache cache, string key) {
        externRemove(cache, key);
    }

    # Updates the cache based on the replace operation related to the LRU eviction algorithm.
    #
    # + cache - A `Cache` object
    # + key - A key, that's value needs to be replaced in the cache
    # + newValue - The new value
    public isolated function replace(Cache cache, string key, any newValue) {}

    # Updates the cache based on the clear operation related to the LRU eviction algorithm.
    #
    # + cache - A `Cache` object
    public isolated function clear(Cache cache) {
        externCleanUp(cache);
    }

    # Updates the cache based on the evict operation.
    public isolated function evict() {}

}

isolated function externPut(Cache cache, string key, any value) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externGet(Cache cache, string key) returns CacheEntry? = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externRemove(Cache cache, string key) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Cache"
} external;

isolated function externCleanUp(Cache cache) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Cache"
} external;
