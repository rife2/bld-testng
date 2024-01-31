# [bld](https://rife2.com/bld) Extension to Run Tests with [TestNG](https://testng.org/)


[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/1.8.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-testng/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-testng)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-testng/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-testng)
[![GitHub CI](https://github.com/rife2/bld-testng/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-testng/actions/workflows/bld.yml)

To install, please refer to the [extensions documentation](https://github.com/rife2/bld/wiki/Extensions).

To run the tests with TestNG, add the following to your build file:

```java
@BuildCommand(summary = "Tests the project with TestNG")
public void testng() throws Exception {
    new TestNgOperation()
    .fromProject(this)
    .packages("com.example")
    .execute();
}
```

```console
./bld compile testng
```

You could also override the default `test` command:

```java
@BuildCommand(summary = "Tests the project with TestNG")
public void test throws Exception {
    new TestNgOperation()
    .fromProject(this)
    .suites("src/test/resources/testng.xml")
    .verbose(2)
    .execute();
}
```

```console
./bld compile test
```

- [View Examples Project](https://github.com/rife2/bld-testng/tree/master/examples)

Please check the [TestNgOperation documentation](https://rife2.github.io/bld-testng/rife/bld/extension/TestNgOperation.html#method-summary) for all available configuration options.

### TestNG Dependency

Don't forget to add a TestNG `test` dependency to your build file, as it is not provided by the extension. For example:

```java
repositories = List.of(MAVEN_CENTRAL);
scope(test).include(dependency("org.testng", "testng", version(7, 9, 0)));

```
