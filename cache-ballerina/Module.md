## Overview

This module provides APIs for in-memory caching by using a semi-persistent mapping from keys to values. Cache entries are added to the cache manually and are stored in the cache until either evicted or invalidated manually.

The existing implementation is based on the Least Recently Used (LRU) eviction algorithm and using a `map` data structure. It is not recommended to insert `()` as the 
value of the cache since it doesn't make sense to cache a nil. 

The cache entries can be safely accessed by multiple concurrent threads as it is thread-safe.

The Cache entries will be evicted in case of the following scenarios:

- When using the `get` API, if the returning cache entry has expired, it gets removed.
- When using the `put` API, if the cache size has reached its capacity, the number of entries that gets removed will be based on the `eviction policy` and the `eviction factor`.
- If `cleanupIntervalInSeconds` (optional property) is configured, the timer task will remove the expired cache 
entries based on the configured interval. The main benefit of this property is that you can optimize the memory 
usage while adding some additional CPU costs and vice versa. The default behaviour is the CPU-optimized method.

The `cache:AbstractCache` object has the common APIs for the caching functionalities. Custom implementations of the cache can be done with different data storages like file, database, etc., with the structural equivalency to the `cache:AbstractCacheObject` object.

```ballerina
public type AbstractCache object {
    public function put(string key, any value, int maxAgeInSeconds) returns Error?;
    public function get(string key) returns any|Error;
    public function invalidate(string key) returns Error?;
    public function invalidateAll() returns Error?;
    public function hasKey(string key) returns boolean;
    public function keys() returns string[];
    public function size() returns int;
    public function capacity() returns int;
};
```
The Ballerina Cache package provides the `cache:Cache` class, which is a `map` data structure based implementation of the `cache:AbstractCache` object.
