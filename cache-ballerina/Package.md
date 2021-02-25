## Package Overview

This package provides APIs for handle caching in Ballerina. It consists of a default implementation based on the LRU eviction algorithm.

The `cache:AbstractCache` object has the common APIs for the caching functionalities. Custom implementations of the cache can be done with different data storages like file, database, etc. with the structural equivalency to the `cache:AbstractCacheObject` object.

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

The Ballerina Cache package provides the `cache:Cache` class, which is a `map` data structure based implementation of the `cache:AbstractCache` object. It is not recommended to insert `()` as the value of the cache since it doesn't make sense to cache a nil.

While initializing the `cache:Cache`, you need to pass the following parameters as the cache configurations.
- `capacity` - Maximum number of entries allowed for the cache
- `evictionFactor` - The factor by which the entries will be evicted once the cache is full
- `evictionPolicy` - The policy which is used to evict entries once the cache is full
- `defaultMaxAgeInSeconds` - Freshness time of all the cache entries in seconds. This value can be overwritten by the
`maxAgeInSeconds` property when inserting an entry to the cache. '-1' means the entries are valid forever.
- `cleanupIntervalInSeconds` - The interval time of the timer task, which cleans the cache entries
This is an optional parameter.

For a better user experience, the above-mentioned configuration is initialized with the below default values:

```ballerina
public type CacheConfig record {|
    int capacity = 100;
    float evictionFactor = 0.25;
    EvictionPolicy evictionPolicy = LRU;
    int defaultMaxAgeInSeconds = -1;
    int cleanupIntervalInSeconds?;
|};
```

There are 2 mandatory scenarios and 1 optional scenario in which a cache entry gets removed from the cache and maintains the freshness of the cache entries. The 2 independent factors (i.e., eviction policy and freshness time of the cache entry) governs the 3 scenarios.

1. When using the `get` API, if the returning cache entry has expired, it gets removed.
2. When using the `put` API, if the cache size has reached its capacity, the number of entries get removed based on the 'eviction policy' and the 'eviction factor'.
3. If `cleanupIntervalInSeconds` (optional property) is configured, the timer task will remove the expired cache entries based on the configured interval.

The main benefit of using the `cleanupIntervalInSeconds` (optional) property is that the developer can optimize the memory usage while adding some additional CPU costs and vice versa. The default behaviour is the CPU-optimized method.

For information on the operations, which you can perform with the cache package, see the below __Functions__. For examples on the usage of the operations, see [Cache Example](https://ballerina.io/learn/by-example/cache.html)
