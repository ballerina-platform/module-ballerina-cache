## Package Overview

This package provides APIs for in-memory caching by using a semi-persistent mapping from keys to values. Cache entries are added to the cache manually and are stored in the cache until either evicted or invalidated manually.

This is based on the Least Recently Used (LRU) eviction algorithm by using a `map` data structure and defining the most basic operations on a collection of cache entries, which entails basic reading, writing, and deleting individual cache items.
It does not allow the `()` as a key or value of the cache and entries can be accessed safely by multiple concurrent threads as it is thread-safe.

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

## Report issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina standard library parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

## Useful links

- Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
