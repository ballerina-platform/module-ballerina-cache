## Overview

This module provides APIs for in-memory caching using a semi-persistent mapping from keys to values, based on the Least Recently Used (LRU) eviction algorithm. Cache entries are added manually and stored until evicted or invalidated, with thread-safe access for concurrent use.

## Key Features

- Manual cache entry addition, retrieval, and invalidation
- Configurable capacity, eviction factor, max age, and cleanup interval
- Thread-safe concurrent access
- Custom cache implementations via the `cache:AbstractCache` object

The cache can be defined with optional configurations as follows:
```ballerina
cache:Cache cache = new (capacity = 10, evictionFactor = 0.2, defaultMaxAge = 0.5, cleanupInterval = 1);
```

The Cache entries will be evicted in case of the following scenarios:

- When using the `get` API, if the returning cache entry has expired, it gets removed.
- When using the `put` API, if the cache size has reached its capacity, the number of entries that get removed will be based on the `eviction policy` and the `eviction factor`.
- If `cleanupInterval` (optional property) is configured, the recurrence task will remove the expired cache entries based on the configured interval. The main benefit of this property is that you can optimize the memory usage while adding some additional CPU costs and vice versa. The default behaviour is the CPU-optimized method.

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
