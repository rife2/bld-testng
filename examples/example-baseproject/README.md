## Compile and Run Tests with TestNG

```console
./bld compile test
```

## Compile and Generate JaCoCo Reports

```console
./bld compile jacoco
```

## Compile and run specific test classes

```console
./bld compile test -testclass=com.example.ExamplesTest,com.example.SampleTest
```

## Compile and run specific test groups

```console
./bld comple test -groups=hello,bonjour
```

## Compile and run specific test methods

```console
./bld compile test -methods=com.example.ExamplesTest.foo,com.example.ExamplesTest.bar
```

## Compile and run specific test name(s)

```console
./bld compile test -testnames="All Packages"
```

## Compile and run tests excluding specific groups

```console
./bld compile test -excludegroups=foo,bar

```

## Compile and run tests with specific verbosity

```console
./bld compile test -log=5

```

## Explore

- [View Build File](https://github.com/rife2/bld-testng/blob/master/examples/example-baseproject/src/bld/java/com/example/ExampleBuild.java)
- [View Wrapper Properties](https://github.com/rife2/bld-testng/blob/master/examples/example-baseproject/lib/bld/bld-wrapper.properties)
