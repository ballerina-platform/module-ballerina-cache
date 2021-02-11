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

# The `cache:AbstractEvictionPolicy` Ballerina  object is used for custom implementations of the eviction policy for a
# Ballerina cache object. Any custom eviction policy implementation should be object-wise similar.
public type AbstractEvictionPolicy object {

    # Updates the cache based on the get operation.
    #
    # + cache - A `Cache` object
    # + key - A key, which is retrieved
    # + return - A cached value associated with the provided key or an `nill`
    public isolated function get(Cache cache, string key) returns any?;

    # Updates the cache based on the put operation.
    #
    # + cache - A `Cache` object
    # + key - A key, which is insert
    # + value - A value, which is added newly
    public isolated function put(Cache cache, string key, any value);

    # Updates the cache based on the remove operation.
    #
    # + cache - A `Cache` object
    # + key - A key, which is deleted
    public isolated function remove(Cache cache, string key);

    # Updates the cache based on the replace operation.
    #
    # + cache - A `Cache` object
    # + key - A key, which will be replacing in the cache
    # + newValue - A value, which will be replaced by the `newValue`
    public isolated function replace(Cache cache, string key, any newValue);

    # Updates the linked list based on the clear operation.
    #
    # + cache - A `Cache` object
    public isolated function clear(Cache cache);

    # Updates the cache based on the evict operation.
    public isolated function evict();

};
