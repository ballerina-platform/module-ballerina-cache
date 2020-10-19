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

import ballerina/java;
//import ballerina/log;

# The `cache:LruEvictionPolicy` object consists of the LRU eviction algorithm related operations based on a linked
# list data structure.
public class LruEvictionPolicy {

    *AbstractEvictionPolicy;
    //LinkedList linkedList;

    # Called when a new `cache:LruEvictionPolicy` object is created.
    public isolated function init() {
        externIni(self);
    }

    # Updates the linked list based on the get operation related to the LRU eviction algorithm.
    #
    # + node - Node of the linked list, which is retrieved
    public isolated function get(Node node) {
        //log:printInfo("****************************");
        externRemoveNode(self, node);
        //log:printInfo("****************************");
        externAddFirst(self, node);
        //log:printInfo("****************************");
    }

    # Updates the linked list based on the put operation related to the LRU eviction algorithm.
    #
    # + node - Node of the linked list, which is added newly
    public isolated function put(Node node) {
        externAddFirst(self, node);
    }

    # Updates the linked list based on the remove operation related to the LRU eviction algorithm.
    #
    # + node - Node of the linked list, which is deleted
    public isolated function remove(Node node) {
        externRemoveNode(self, node);
    }

    # Updates the linked list based on the replace operation related to the LRU eviction algorithm.
    #
    # + newNode - Node of the linked list, which will be replacing the `oldNode`
    # + oldNode - Node of the linked list, which will be replaced by the `newNode`
    public isolated function replace(Node newNode, Node oldNode) {
        externRemoveNode(self, oldNode);
        externAddFirst(self, newNode);
    }

    # Updates the linked list based on the clear operation related to the LRU eviction algorithm.
    public isolated function clear() {
        externClear(self);
    }

    # Updates the linked list based on the evict operation.
    # + return - The Node, which is evicted from the linked list or `()` if nothing to be evicted
    public isolated function evict() returns CacheEntry? {
        return externRemoveLast(self);
    }

}

isolated function externIni(LruEvictionPolicy lruEvictionPolicy) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"
} external;

isolated function externAddLast(LruEvictionPolicy lruEvictionPolicy, Node node) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"
} external;

isolated function externAddFirst(LruEvictionPolicy lruEvictionPolicy, Node node) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"
} external;

isolated function externRemoveLast(LruEvictionPolicy lruEvictionPolicy) returns CacheEntry? = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"
} external;

isolated function externRemoveNode(LruEvictionPolicy lruEvictionPolicy, Node node) = @java:Method {
    name: "externRemove",
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"

} external;

isolated function externClear(LruEvictionPolicy lruEvictionPolicy) = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.LinkedList"
} external;
