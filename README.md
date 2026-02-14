# [bld](https://rife2.com/bld) Extension to Run Tests with [TestNG](https://testng.org/)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-testng%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-testng)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-testng%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-testng)
[![GitHub CI](https://github.com/rife2/bld-testng/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-testng/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-testng=com.uwyn.rife2:bld-testng
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.

## Test with TestNG

To execute tests with TestNG, add the following to your build file:

```java
@Override
public void test() throws Exception {
    new TestNgOperation()
            .fromProject(this)
            .packages("com.example")
            .execute();
}
```
- [View Example Project](https://github.com/rife2/bld-testng/tree/master/examples/example-project)


or if using [BaseProject](https://rife2.github.io/bld/rife/bld/BaseProject.html): 

```java
@Override
public TestOperation<?, ?> testOperation() {
    return new TestNgOperation()
            .fromProject(this)
            .packages("com.example");
}
```
- [View Example Project](https://github.com/rife2/bld-testng/tree/master/examples/example-baseproject)

To run tests:

```console
./bld compile test

./bld compile test -testclass=com.example.Test,com.sample.Test
./bld compile test -methods=com.example.Test.foo,com.example.Test.bar
./bld compile test -testnames=Test1,Test2
./bld compile test -groups=group1,group2
./bld compile test -excludegroups=group1,group3
./bld compile test -log=5
```


Please check the [TestNgOperation documentation](https://rife2.github.io/bld-testng/rife/bld/extension/TestNgOperation.html#method-summary) for all available configuration options.

### TestNG Dependency

Don't forget to add a TestNG `test` dependency to your build file, as it is not provided by the extension. For example:

```java
repositories = List.of(MAVEN_CENTRAL);
scope(test).include(dependency("org.testng", "testng", version(7, 12, 0)));
```
