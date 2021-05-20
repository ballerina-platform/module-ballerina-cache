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

// This is a linked list data structure implementation, which is used for the eviction algorithm of the cache.

# Represents a structure to keep data and references to the adjacent nodes of the linked list.
#
# + value - Value to be stored in the linked list node
# + prev - Previous node of the linked list
# + next - Next node of the linked list
public type Node record {|
    CacheEntry value;
    Node? prev = ();
    Node? next = ();
|};

# The `cache:LinkedList` object consists operations of `LinkedList` data structure which are related
# to LRU eviction algorithm
#
# + head - The first node of the linked list
# + tail - The last node of the linked list
public isolated class LinkedList {

    private Node? head = ();
    private Node? tail = ();

    # Adds a node to the end of the provided linked list.
    #
    # + node - The node, which should be added to the provided linked list
    isolated function addLast(Node node) {
        lock {
            if (tryLock()) {
                if (self.tail is ()) {
                    //value:Cloneable cloneableValue = node.clone();
                    //anydata node1 = node.cloneReadOnly();
                    self.head = node.clone();
                    self.tail = self.head.clone();
                    releaseLock();
                    return;
                }
                Node tempNode = node.clone();
                Node tailNode = <Node>self.tail;
                tempNode.prev = tailNode;
                tailNode.next = tempNode;
                self.tail = tempNode;
                releaseLock();
            }
        }
    }

    # Adds a node to the start of the provided linked list.
    #
    # + node - The node, which should be added to the provided linked list
    isolated function addFirst(Node node) {
        lock {
            if (tryLock()) {
                if (self.head is ()) {
                    self.head = node.clone();
                    self.tail = self.head.clone();
                    releaseLock();
                    return;
                }
                Node tempNode = node.clone();
                Node headNode = <Node>self.head;
                tempNode.next = headNode;
                headNode.prev = tempNode;
                self.head = tempNode;
                releaseLock();
            }
        }
    }

    # Removes a node from the provided linked list.
    #
    # + node - The node, which should be removed from the provided linked list
    isolated function remove(Node node) {
        if (tryLock()) {
            if (node.prev is ()) {
                lock {
                    self.head = node.next.clone();
                }
            } else {
                Node prev;
                lock {
                    prev = <Node>node.prev.clone();
                }
                prev.next = node.next;
            }
            if (node.next is ()) {
                lock {
                    self.tail = node.prev.clone();
                }

            } else {
                Node next = <Node>node.next.clone();
                next.prev = node.prev;
            }
            node.next = ();
            node.prev = ();
            releaseLock();
        }
    }

    # Removes the last node from the provided linked list.
    #
    # + return - Last node of the provided linked list or `()` if the last node is empty
    isolated function removeLast() returns Node? {
        lock{
            if (self.tail is ()) {
                return ();
            }
        }
        Node tail;
        lock {
            tail = <Node>self.tail.clone();
        }
        lock {
            self.remove(tail);
        }
        return tail;
    }

    # Clears the provided linked list.
    isolated function clear() {
        lock{
            if (tryLock()) {
                self.head = ().clone();
                self.tail = ().clone();
                releaseLock();
            }
        }
    }
}

isolated function externLockInit() = @java:Method {
    name: "init",
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Lock"
} external;

isolated function tryLock() returns boolean = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Lock"
} external;

isolated function releaseLock() = @java:Method {
    'class: "org.ballerinalang.stdlib.cache.nativeimpl.Lock"
} external;
