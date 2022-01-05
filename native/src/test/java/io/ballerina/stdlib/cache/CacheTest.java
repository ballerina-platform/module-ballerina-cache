/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.cache;

import io.ballerina.stdlib.cache.nativeimpl.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Test native functions for concurrent linked hash map call.
 */
public class CacheTest {

    private ConcurrentLinkedHashMap<String, String> cacheMap;

    @BeforeTest()
    public void setupCache() {
        cacheMap = new ConcurrentLinkedHashMap<>(10);
    }

    @Test()
    public void testPutWithNullKey() {
        Assert.assertNull(cacheMap.put(null, "value"));
    }

    @Test(description = "")
    public void testPutWithNullValue() {
        Assert.assertNull(cacheMap.put("key", null));
    }

    @Test(description = "")
    public void testContainsKeyWithNullKey() {
        Assert.assertFalse(cacheMap.containsKey(null));
    }

    @Test()
    public void testGetWithNullKey() {
        Assert.assertNull(cacheMap.get(null));
    }

    @Test()
    public void testRemoveWithNullKey() {
        Assert.assertNull(cacheMap.remove(null));
    }
}
