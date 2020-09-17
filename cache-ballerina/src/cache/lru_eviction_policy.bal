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

# The `cache:LruEvictionPolicy` object consists of the LRU eviction algorithm related operations based on a linked
# list data structure.
public class LruEvictionPolicy {

    *AbstractEvictionPolicy;
    private LruLinkedList linkedList = new LruLinkedList();

    # Updates the linked list based on the get operation related to the LRU eviction algorithm.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    # + node - Node of the linked list, which is retrieved
    public isolated function get(LinkedList list, Node node) {
        self.linkedList.remove(list, node);
        self.linkedList.addFirst(list, node);
    }

    # Updates the linked list based on the put operation related to the LRU eviction algorithm.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    # + node - Node of the linked list, which is added newly
    public isolated function put(LinkedList list, Node node) {
        self.linkedList.addFirst(list, node);
    }

    # Updates the linked list based on the remove operation related to the LRU eviction algorithm.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    # + node - Node of the linked list, which is deleted
    public isolated function remove(LinkedList list, Node node) {
        self.linkedList.remove(list, node);
    }

    # Updates the linked list based on the replace operation related to the LRU eviction algorithm.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    # + newNode - Node of the linked list, which will be replacing the `oldNode`
    # + oldNode - Node of the linked list, which will be replaced by the `newNode`
    public isolated function replace(LinkedList list, Node newNode, Node oldNode) {
        self.linkedList.remove(list, oldNode);
        self.linkedList.addFirst(list, newNode);
    }

    # Updates the linked list based on the clear operation related to the LRU eviction algorithm.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    public isolated function clear(LinkedList list) {
        self.linkedList.clear(list);
    }

    # Updates the linked list based on the evict operation.
    #
    # + list - Linked list data structure, which is used to govern the eviction policy
    # + return - The Node, which is evicted from the linked list or `()` if nothing to be evicted
    public isolated function evict(LinkedList list) returns Node? {
        return self.linkedList.removeLast(list);
    }

}
