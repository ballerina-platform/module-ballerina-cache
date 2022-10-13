Ballerina Cache Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-cache/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-cache/actions/workflows/build-timestamped-master.yml)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-cache/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-cache/actions/workflows/trivy-scan.yml)  
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-cache.svg)](https://github.com/ballerina-platform/module-ballerina-cache/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/cache.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fcache)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-cache/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-cache)


This library provides APIs for in-memory caching by using a semi-persistent mapping from keys to values. Cache entries are added to the cache manually and are stored in the cache until either evicted or invalidated manually.

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

For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example).

## Issues and projects 

Issues and Project are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the package.

## Build from the source

### Set up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).
   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
   
   * [OpenJDK](https://adoptium.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Build the source

Execute the commands below to build from the source.

1. To build the library:
        
        ./gradlew clean build
        
2. To run the tests:

        ./gradlew clean test
        
3. To build the package without tests:

        ./gradlew clean build -x test

4. To run a group of tests:

        ./gradlew clean test -Pgroups=<test_group_names>

5. To debug package implementation:

        ./gradlew clean build -Pdebug=<port>
        
6. To debug the package with Ballerina language:

        ./gradlew clean build -PbalJavaDebug=<port>

7. Publish ZIP artifact to the local `.m2` repository:

        ./gradlew clean build publishToMavenLocal

   
8. Publish the generated artifacts to the local Ballerina central repository:

        ./gradlew clean build -PpublishToLocalCentral=true
        
9. Publish the generated artifacts to the Ballerina central repository:

        ./gradlew clean build -PpublishToCentral=true

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`cache` library](https://lib.ballerina.io/ballerina/cache/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
