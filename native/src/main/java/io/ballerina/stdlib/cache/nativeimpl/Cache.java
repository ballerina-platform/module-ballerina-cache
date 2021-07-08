/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.cache.nativeimpl;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.cache.nativeimpl.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.Map;

/**
 * Class to handle ballerina external functions in Cache library.
 *
 * @since 2.0.0
 */
public class Cache {

    private static ConcurrentLinkedHashMap<BString, BMap<BString, Object>> cacheMap;
    private static final String MAX_CAPACITY = "maxCapacity";
    private static final String EVICTION_FACTOR = "evictionFactor";
    private static final String EXPIRE_TIME = "expTime";
    private static final String CACHE = "CACHE";

    private Cache() {}

    public static void externInit(BObject cache) {
        int capacity = (int) cache.getIntValue(StringUtils.fromString(MAX_CAPACITY));
        cacheMap = new ConcurrentLinkedHashMap<>(capacity);
        cache.addNativeData(CACHE, cacheMap);
    }

    @SuppressWarnings("unchecked")
    public static void externPut(BObject cache, BString key, BMap<BString, Object> value) {
        int capacity = (int) cache.getIntValue(StringUtils.fromString(MAX_CAPACITY));
        float evictionFactor = (float) cache.getFloatValue(StringUtils.fromString(EVICTION_FACTOR));
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        if (cacheMap.size() >= capacity) {
            int evictionKeysCount = (int) (capacity * evictionFactor);
                cacheMap.setCapacity((capacity - evictionKeysCount));
                cacheMap.setCapacity(capacity);
        }
        cacheMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static BMap<BString, Object> externGet(BObject cache, BString key, BDecimal currentTime) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        BMap<BString, Object> value = cacheMap.get(key);
        Long time = ((BDecimal) value.get(StringUtils.fromString(EXPIRE_TIME))).decimalValue().longValue();
        if (time != -1 && time <= currentTime.decimalValue().longValue()) {
            cacheMap.remove(key);
            return null;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static void externRemove(BObject cache, BString key) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        cacheMap.remove(key);
    }

    @SuppressWarnings("unchecked")
    public static void externRemoveAll(BObject cache) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        cacheMap.clear();
    }

    @SuppressWarnings("unchecked")
    public static boolean externHasKey(BObject cache, BString key) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        return cacheMap.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public static BArray externKeys(BObject cache) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        return ValueCreator.createArrayValue(cacheMap.keySet().toArray(new BString[0]));
    }

    @SuppressWarnings("unchecked")
    public static int externSize(BObject cache) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        return cacheMap.size();
    }

    @SuppressWarnings("unchecked")
    public static void externCleanUp(BObject cache, BDecimal currentTime) {
        cacheMap = (ConcurrentLinkedHashMap<BString, BMap<BString, Object>>) cache.getNativeData(CACHE);
        for (Map.Entry<BString, BMap<BString, Object>> entry : cacheMap.entrySet()) {
            BMap<BString, Object> value = entry.getValue();
            Long time = ((BDecimal) value.get(StringUtils.fromString(EXPIRE_TIME))).decimalValue().longValue();
            if (time != -1 && time <= currentTime.decimalValue().longValue()) {
                cacheMap.remove(entry.getKey());
            }
        }
    }
}
